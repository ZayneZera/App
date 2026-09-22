package com.zayne.seedfilter;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Runs the native seed-filter-engine.exe (C + cubiomes, see seed-filter-engine/ in the repo)
 * in headless mode and parses its result - all criteria checking now lives there instead of in
 * this mod, since it's immune to the reimplementation bugs the old Java scanner needed several
 * rounds of fixes for. Settings are configured through the exe's own window, not in-game.
 */
public class ExternalEngine {

    private static final Logger LOGGER = LogManager.getLogger("seed-filter");

    /** Mirrors config.h's CATEGORY_* bit flags exactly - which top-level category(ies) a seed
     * matched. In AND-mode searches (the normal join-now flow) this is always exactly the full
     * enabled set; in OR-mode (the seed bank) it can be any non-empty subset, and more than one
     * bit set is what the seed bank tags "OP". */
    public static final int CATEGORY_VILLAGE = 1;
    public static final int CATEGORY_RUINED_PORTAL = 1 << 1;
    public static final int CATEGORY_TREASURE = 1 << 2;
    public static final int CATEGORY_BASTION = 1 << 3;
    public static final int CATEGORY_FORTRESS = 1 << 4;

    public static class Result {
        public final long seed;
        public final int spawnX;
        public final int spawnZ;
        public final boolean cheats;
        public final boolean creative;

        /** Only set (rpFound=true) when the exe was run with ruined_portal_frame_check=1 and the
         * found portal rolled a checkable placement - see RuinedPortalVerifier. */
        public boolean rpFound = false;
        public int rpPortalX, rpPortalZ;
        public int rpTemplateIndex, rpRotation, rpMirror;
        public int rpChestObsidian;

        /** Bitwise-OR of the CATEGORY_* flags above. Defaults to 0 for a result parsed from a
         * pre-OR-mode exe build (no MatchedCategories: line) - callers that need it should treat
         * 0 here as "unknown / assume fully matched", not "matched nothing" (a genuine 0 can never
         * reach a Result at all, since the exe only reports a match when at least one bit is set). */
        public int matchedCategories = 0;

        public Result(long seed, int spawnX, int spawnZ, boolean cheats, boolean creative) {
            this.seed = seed;
            this.spawnX = spawnX;
            this.spawnZ = spawnZ;
            this.cheats = cheats;
            this.creative = creative;
        }
    }

    /** Accumulates one result's fields as its "Key: value" lines stream in from the exe's stdout -
     * shared by runAsync (one shot, builds on process exit) and runBankSearch (streaming, builds
     * on each "MatchEnd:" line and then resets for the next one). */
    private static final class ResultAccumulator {
        Long seed;
        Integer spawnX, spawnZ;
        boolean cheats, creative;
        Integer rpPortalX, rpPortalZ, rpTemplateIndex, rpRotation, rpMirror, rpChestObsidian;
        int matchedCategories = 0;

        /** Returns true if the line was one of ours (consumed), false if the caller should
         * handle it itself (e.g. "Progress:"). */
        boolean consume(String line) {
            if (line.startsWith("Seed:")) {
                seed = Long.parseLong(line.substring("Seed:".length()).trim());
            } else if (line.startsWith("SpawnX:")) {
                spawnX = Integer.parseInt(line.substring("SpawnX:".length()).trim());
            } else if (line.startsWith("SpawnZ:")) {
                spawnZ = Integer.parseInt(line.substring("SpawnZ:".length()).trim());
            } else if (line.startsWith("Cheats:")) {
                cheats = line.substring("Cheats:".length()).trim().equals("1");
            } else if (line.startsWith("Creative:")) {
                creative = line.substring("Creative:".length()).trim().equals("1");
            } else if (line.startsWith("RpPortalX:")) {
                rpPortalX = Integer.parseInt(line.substring("RpPortalX:".length()).trim());
            } else if (line.startsWith("RpPortalZ:")) {
                rpPortalZ = Integer.parseInt(line.substring("RpPortalZ:".length()).trim());
            } else if (line.startsWith("RpTemplateIndex:")) {
                rpTemplateIndex = Integer.parseInt(line.substring("RpTemplateIndex:".length()).trim());
            } else if (line.startsWith("RpRotation:")) {
                rpRotation = Integer.parseInt(line.substring("RpRotation:".length()).trim());
            } else if (line.startsWith("RpMirror:")) {
                rpMirror = Integer.parseInt(line.substring("RpMirror:".length()).trim());
            } else if (line.startsWith("RpChestObsidian:")) {
                rpChestObsidian = Integer.parseInt(line.substring("RpChestObsidian:".length()).trim());
            } else if (line.startsWith("MatchedCategories:")) {
                matchedCategories = Integer.parseInt(line.substring("MatchedCategories:".length()).trim());
            } else {
                return false;
            }
            return true;
        }

        /** Null if seed/spawnX/spawnZ haven't all been seen yet. */
        Result buildIfComplete() {
            if (seed == null || spawnX == null || spawnZ == null) {
                return null;
            }
            Result result = new Result(seed, spawnX, spawnZ, cheats, creative);
            result.matchedCategories = matchedCategories;
            if (rpPortalX != null && rpPortalZ != null && rpTemplateIndex != null && rpRotation != null && rpMirror != null && rpChestObsidian != null) {
                result.rpFound = true;
                result.rpPortalX = rpPortalX;
                result.rpPortalZ = rpPortalZ;
                result.rpTemplateIndex = rpTemplateIndex;
                result.rpRotation = rpRotation;
                result.rpMirror = rpMirror;
                result.rpChestObsidian = rpChestObsidian;
            }
            return result;
        }

        void reset() {
            seed = null;
            spawnX = null;
            spawnZ = null;
            cheats = false;
            creative = false;
            rpPortalX = null;
            rpPortalZ = null;
            rpTemplateIndex = null;
            rpRotation = null;
            rpMirror = null;
            rpChestObsidian = null;
            matchedCategories = 0;
        }
    }

    public static Path getEngineDir() {
        return FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("seedfilter");
    }

    public static Path getExePath() {
        return getEngineDir().resolve("seedfilter.exe");
    }

    public static Path getConfigPath() {
        return getEngineDir().resolve("seedfilter.cfg");
    }

    public static boolean isInstalled() {
        return Files.isRegularFile(getExePath());
    }

    /** Opens the exe's own settings window (no arguments = GUI mode), does not wait for it. */
    public static void openSettingsGui() {
        try {
            new ProcessBuilder(getExePath().toString())
                    .directory(getEngineDir().toFile())
                    .start();
        } catch (IOException e) {
            LOGGER.error("Failed to launch seedfilter.exe", e);
        }
    }

    /**
     * Runs "seedfilter.exe --search <config>" on a background thread and completes the future
     * with the parsed result. The AtomicReference<Process> lets a caller cancel by destroying
     * the process (see cancel()). stats (may be null) is updated live as "Progress:" lines
     * stream in, so a screen can poll it for a live funnel display.
     */
    public static CompletableFuture<Result> runAsync(AtomicReference<Process> processHolder, EngineStats stats) {
        CompletableFuture<Result> future = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                Process process = new ProcessBuilder(
                        getExePath().toString(), "--search", getConfigPath().toString())
                        .directory(getEngineDir().toFile())
                        .redirectErrorStream(true)
                        .start();
                processHolder.set(process);
                // Belt and suspenders against orphaning the exe (and its worker threads still
                // spinning at full CPU) if the game closes entirely while a search is running -
                // any path that skips ExternalEngine.cancel() would otherwise leave it running
                // forever with no UI left pointing at it.
                Thread killOnJvmExit = new Thread(process::destroy);
                Runtime.getRuntime().addShutdownHook(killOnJvmExit);

                ResultAccumulator acc = new ResultAccumulator();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!acc.consume(line) && line.startsWith("Progress:") && stats != null) {
                            stats.applyProgressLine(line);
                        }
                    }
                }

                process.waitFor();
                try {
                    Runtime.getRuntime().removeShutdownHook(killOnJvmExit);
                } catch (IllegalStateException ignored) {
                    // JVM is already shutting down - the hook will run (or has run) regardless.
                }

                Result result = acc.buildIfComplete();
                if (result != null) {
                    future.complete(result);
                } else {
                    future.completeExceptionally(new IOException("seedfilter.exe returned no seed (cancelled or NoMatch)"));
                }
            } catch (IOException | InterruptedException | NumberFormatException e) {
                future.completeExceptionally(e);
            }
        }, "seedfilter-engine-caller");
        thread.setDaemon(true);
        thread.start();

        return future;
    }

    /**
     * Runs "seedfilter.exe --search-bank <config>" on a background thread for the seed bank's
     * "find and save, don't join" mode: unlike runAsync, the process never exits after the first
     * match - onMatch is invoked once per match as they stream in, indefinitely, until the caller
     * cancels (via cancel(processHolder), same as runAsync) or the process otherwise exits.
     * onProcessExit always runs exactly once when reading stops, cancelled or not, so a screen can
     * reset its UI state (e.g. re-enable its start button) either way.
     */
    public static void runBankSearch(AtomicReference<Process> processHolder, EngineStats stats,
                                      Consumer<Result> onMatch, Runnable onProcessExit) {
        Thread thread = new Thread(() -> {
            try {
                Process process = new ProcessBuilder(
                        getExePath().toString(), "--search-bank", getConfigPath().toString())
                        .directory(getEngineDir().toFile())
                        .redirectErrorStream(true)
                        .start();
                processHolder.set(process);
                Thread killOnJvmExit = new Thread(process::destroy);
                Runtime.getRuntime().addShutdownHook(killOnJvmExit);

                ResultAccumulator acc = new ResultAccumulator();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("MatchEnd:")) {
                            Result result = acc.buildIfComplete();
                            if (result != null) {
                                onMatch.accept(result);
                            }
                            acc.reset();
                        } else if (!acc.consume(line) && line.startsWith("Progress:") && stats != null) {
                            stats.applyProgressLine(line);
                        }
                    }
                }

                process.waitFor();
                try {
                    Runtime.getRuntime().removeShutdownHook(killOnJvmExit);
                } catch (IllegalStateException ignored) {
                    // JVM is already shutting down - the hook will run (or has run) regardless.
                }
            } catch (IOException | InterruptedException | NumberFormatException e) {
                LOGGER.warn("Seed bank background search ended", e);
            } finally {
                if (onProcessExit != null) {
                    onProcessExit.run();
                }
            }
        }, "seedfilter-bank-caller");
        thread.setDaemon(true);
        thread.start();
    }

    public static void cancel(AtomicReference<Process> processHolder) {
        Process process = processHolder.get();
        if (process != null) {
            process.destroy();
        }
    }
}
