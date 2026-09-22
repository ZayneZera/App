package com.zayne.seedfilter;

import com.zayne.seedfilter.gui.CountdownScreen;
import com.zayne.seedfilter.gui.EngineMissingScreen;
import com.zayne.seedfilter.gui.ScanProgressScreen;
import com.zayne.seedfilter.mixin.CreateWorldScreenAccessor;
import com.zayne.seedfilter.mixin.MoreOptionsDialogAccessor;
import com.zayne.seedfilter.util.WorldNaming;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 * All criteria filtering now lives in the external seedfilter.exe (see seed-filter-engine/ in
 * the repo) - this mod is just the in-game trigger + world-creation glue around it.
 */
public class SeedFilterMod implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("seed-filter");

    @Override
    public void onInitializeClient() {
        MenuHotkey.register();
    }

    public static void startScanAndCreate(MinecraftClient client, Screen titleScreen) {
        if (!ExternalEngine.isInstalled()) {
            client.openScreen(new EngineMissingScreen(titleScreen));
            return;
        }

        AtomicReference<Process> processHolder = new AtomicReference<>();
        EngineStats stats = new EngineStats();
        client.openScreen(new ScanProgressScreen(titleScreen, processHolder, stats));

        ExternalEngine.runAsync(processHolder, stats).thenAccept(result ->
                client.execute(() -> createAndJoin(client, titleScreen, result))
        ).exceptionally(error -> {
            LOGGER.warn("Seed search did not produce a result", error);
            return null;
        });
    }

    /** Max automatic re-searches after a "Not Approved" Ruined Portal verdict before giving up
     * and just leaving the player on the last (unapproved) seed - guards against looping forever
     * if a truly completable portal is rare for the configured criteria. */
    private static final int MAX_RP_RETRY_ATTEMPTS = 30;

    public static void createAndJoin(MinecraftClient client, Screen titleScreen, ExternalEngine.Result result) {
        createAndJoin(client, titleScreen, result, 1);
    }

    /** attempt is threaded through by RuinedPortalVerifier's auto-retry (via CountdownScreen) so
     * MAX_RP_RETRY_ATTEMPTS counts correctly across repeated "Not Approved" loops instead of
     * resetting to 1 on every retry. */
    public static void createAndJoin(MinecraftClient client, Screen titleScreen, ExternalEngine.Result result, int attempt) {
        CreateWorldScreen screen = new CreateWorldScreen(titleScreen);
        client.openScreen(screen);

        CreateWorldScreenAccessor accessor = (CreateWorldScreenAccessor) screen;
        accessor.getLevelNameField().setText(WorldNaming.nextName());

        MoreOptionsDialog dialog = accessor.getMoreOptionsDialog();
        ((MoreOptionsDialogAccessor) dialog).getSeedTextField().setText(String.valueOf(result.seed));

        if (result.cheats) {
            accessor.setCheatsEnabled(true);
        }

        if (result.creative) {
            applyCreativeDefault(screen);
        }

        // "Nächster Seed" (CountdownScreen path) was reported as never showing the spawn-offset
        // or Ruined-Portal-verdict chat messages, while a fresh game launch (startScanAndCreate)
        // always does - yet no exception ever showed up in logs/latest.log for the failing case,
        // so whatever's happening isn't throwing. These markers pin down exactly how far
        // createAndJoin gets before things go quiet, instead of guessing blind from outside.
        LOGGER.info("createAndJoin: invoking createLevel for seed {} (attempt {})", result.seed, attempt);
        accessor.invokeCreateLevel();
        LOGGER.info("createAndJoin: createLevel invoked, calling announceSpawnOffset");
        announceSpawnOffset(client, result.spawnX, result.spawnZ);
        LOGGER.info("createAndJoin: calling RuinedPortalVerifier.verifyAndAnnounce");
        RuinedPortalVerifier.verifyAndAnnounce(client, result, SeedFilterConfig.load(ExternalEngine.getConfigPath()),
                () -> retryForNewSeed(client, attempt));
        LOGGER.info("createAndJoin: verifyAndAnnounce call returned (method itself may still be polling)");
    }

    /** Called when RuinedPortalVerifier comes back "Not Approved" (or can't find the portal at
     * all) - reuses the same disconnect/search/rejoin flow as the pause-menu "Nächster Seed"
     * button (CountdownScreen), so this gets its progress display and working Abbrechen/Escape
     * cancel button for free instead of running invisibly in the background with no way to stop
     * it. Loops until an actually-completable portal is found or MAX_RP_RETRY_ATTEMPTS is hit. */
    private static void retryForNewSeed(MinecraftClient client, int previousAttempt) {
        int attempt = previousAttempt + 1;
        if (attempt > MAX_RP_RETRY_ATTEMPTS) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(new LiteralText("§c[SeedFilter] Kein completables Ruined Portal nach "
                            + MAX_RP_RETRY_ATTEMPTS + " Versuchen gefunden - breche ab.").formatted(Formatting.RED), false);
                }
            });
            return;
        }
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(new LiteralText("§7[SeedFilter] Nicht completable, suche weiter... (Versuch "
                        + attempt + ")").formatted(Formatting.GRAY), false);
            }
            client.openScreen(new CountdownScreen(attempt));
        });
    }

    /**
     * Sets CreateWorldScreen's Survival/Creative/Hardcore selector field to Creative, so
     * createLevel() picks Creative as the actual world game mode.
     *
     * That field's exact name turned out to be a moving target across Yarn mapping builds (our
     * project pins 1.16.1+build.21, but the name we looked up came from a later build and
     * didn't exist yet in build.21 - "Could not locate @Accessor target currentMode"). Rather
     * than keep guessing names per build, this scans CreateWorldScreen's own declared fields for
     * one whose TYPE is a nested enum declared inside CreateWorldScreen itself (there's only one
     * such field - the mode selector), then within that enum's constants finds whichever one
     * carries a field of type GameMode equal to GameMode.CREATIVE. Field/type *names* changing
     * between mapping builds doesn't affect this at all, since nothing here is looked up by name.
     */
    private static void applyCreativeDefault(CreateWorldScreen screen) {
        try {
            for (Field field : CreateWorldScreen.class.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!fieldType.isEnum() || fieldType.getEnclosingClass() != CreateWorldScreen.class) {
                    continue;
                }
                Field gameModeField = findFieldOfType(fieldType, GameMode.class);
                if (gameModeField == null) {
                    continue;
                }
                gameModeField.setAccessible(true);
                for (Object candidate : fieldType.getEnumConstants()) {
                    if (gameModeField.get(candidate) == GameMode.CREATIVE) {
                        field.setAccessible(true);
                        field.set(screen, candidate);
                        return;
                    }
                }
            }
            LOGGER.warn("Could not find a Creative mode field on CreateWorldScreen via reflection");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to set Creative game mode default", e);
        }
    }

    private static Field findFieldOfType(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() == type) {
                return f;
            }
        }
        return null;
    }

    /**
     * Polls (via self-requeuing client.execute) until the player has actually spawned in, waits
     * a few extra ticks for the position to settle (right when the player entity first appears,
     * its position can still be a temporary placeholder before the terrain-height snap happens),
     * then compares it against the engine's calculated spawn itself and posts the offset - no
     * more manual F3 comparison needed.
     */
    private static void announceSpawnOffset(MinecraftClient client, int calcX, int calcZ) {
        announceSpawnOffset(client, calcX, calcZ, 10, 0);
    }

    private static void announceSpawnOffset(MinecraftClient client, int calcX, int calcZ, int settleTicks, int pollCount) {
        client.execute(() -> {
            if (client.player == null) {
                // A one-shot warning if this is still waiting after ~10s (200 ticks) - if the
                // "Nächster Seed" no-spawn-offset report is this poll never resolving rather than
                // never starting, this pins that down without spamming a log line every tick.
                if (pollCount == 200) {
                    LOGGER.warn("announceSpawnOffset: still waiting for client.player after 200 polls");
                }
                announceSpawnOffset(client, calcX, calcZ, settleTicks, pollCount + 1);
                return;
            }
            if (settleTicks > 0) {
                announceSpawnOffset(client, calcX, calcZ, settleTicks - 1, pollCount + 1);
                return;
            }
            BlockPos pos = client.player.getBlockPos();
            int dx = Math.abs(pos.getX() - calcX);
            int dz = Math.abs(pos.getZ() - calcZ);
            int total = dx + dz;
            client.player.sendMessage(new LiteralText(
                    "§e[SeedFilter] Calculated Offset: " + dx + " + " + dz + " = " + total + " Blöcke"), false);
        });
    }
}
