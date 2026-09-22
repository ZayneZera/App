package com.zayne.seedfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persists the seeds the seed bank's background search found (see ExternalEngine.runBankSearch)
 * without joining them - a flat JSON list on disk, grouped in memory into "stacks" for display: a
 * stack is every saved entry that shares both the exact search settings that found it (signature)
 * and the exact set of categories it matched (matchedCategories), so e.g. a plain "Village" hit
 * and an "OP" Village+Fortress hit from the very same search run land in different stacks.
 */
public class SeedBank {

    private static final Logger LOGGER = LogManager.getLogger("seed-filter");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ENTRY_LIST_TYPE = new TypeToken<ArrayList<SeedBankEntry>>() {}.getType();

    private final Path path;
    private final List<SeedBankEntry> entries;

    private SeedBank(Path path, List<SeedBankEntry> entries) {
        this.path = path;
        this.entries = entries;
    }

    public static SeedBank load(Path path) {
        if (!Files.isRegularFile(path)) {
            return new SeedBank(path, new ArrayList<>());
        }
        try {
            String json = new String(Files.readAllBytes(path));
            List<SeedBankEntry> parsed = GSON.fromJson(json, ENTRY_LIST_TYPE);
            return new SeedBank(path, parsed != null ? parsed : new ArrayList<>());
        } catch (IOException | com.google.gson.JsonSyntaxException e) {
            LOGGER.warn("Failed to load seed bank from {}, starting empty", path, e);
            return new SeedBank(path, new ArrayList<>());
        }
    }

    public void save() {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, GSON.toJson(entries, ENTRY_LIST_TYPE).getBytes());
        } catch (IOException e) {
            LOGGER.warn("Failed to save seed bank to {}", path, e);
        }
    }

    /** Adds and immediately persists one entry - called once per match as the background search
     * streams them in, so a crash/kill mid-search never loses seeds already found. */
    public void add(SeedBankEntry entry) {
        entries.add(entry);
        save();
    }

    public void markUsed(SeedBankEntry entry) {
        entry.used = true;
        save();
    }

    /** Canonical snapshot of the enabled categories' settings - the stacking/stats key.
     * Deliberately excludes cheats/creative/threadCount/orMode, none of which affect which seeds
     * match, so two searches differing only in those still stack together. */
    public static String buildSignature(SeedFilterConfig cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("V").append(cfg.villageEnabled ? cfg.villageMaxChunks : -1).append(';');
        sb.append("RP").append(cfg.ruinedPortalEnabled ? cfg.ruinedPortalMaxChunks : -1)
                .append(cfg.ruinedPortalEnabled && cfg.ruinedPortalLootingSword ? "L" : "").append(';');
        sb.append("BT").append(cfg.buriedTreasureEnabled ? cfg.buriedTreasureMaxChunks : -1)
                .append(",tnt").append(cfg.buriedTreasureEnabled ? cfg.buriedTreasureMinTnt : 0)
                .append(cfg.buriedTreasureEnabled && cfg.buriedTreasureDiamondFilter ? ",dia" : "")
                .append(cfg.buriedTreasureEnabled && cfg.buriedTreasureIronFilter ? ",iron" : "")
                .append(",fish").append(cfg.buriedTreasureEnabled ? cfg.buriedTreasureMinFish : 0).append(';');
        sb.append("BA").append(cfg.bastionEnabled ? cfg.bastionMaxNetherChunks : -1)
                .append(cfg.bastionEnabled && cfg.bastionAllowBridge ? "b" : "")
                .append(cfg.bastionEnabled && cfg.bastionAllowHousing ? "h" : "")
                .append(cfg.bastionEnabled && cfg.bastionAllowStables ? "s" : "")
                .append(cfg.bastionEnabled && cfg.bastionAllowTreasure ? "t" : "").append(';');
        sb.append("FO").append(cfg.fortressEnabled ? cfg.fortressMaxNetherChunks : -1);
        return sb.toString();
    }

    public static final class StackKey {
        public final String signature;
        public final int matchedCategories;

        public StackKey(String signature, int matchedCategories) {
            this.signature = signature;
            this.matchedCategories = matchedCategories;
        }

        /** True when this stack matched more of Village/RuinedPortal/BuriedTreasure than the
         * required minimum at once - the seed bank's "OP". See ExternalEngine.isOp for why
         * Bastion/Fortress never count toward this (they're always mandatory, never a bonus). */
        public boolean isOp() {
            return ExternalEngine.isOp(matchedCategories);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof StackKey)) return false;
            StackKey other = (StackKey) o;
            return matchedCategories == other.matchedCategories && signature.equals(other.signature);
        }

        @Override
        public int hashCode() {
            return Objects.hash(signature, matchedCategories);
        }
    }

    public static final class Stack {
        public final StackKey key;
        /** Unused entries, oldest first - index 0 is what a left-click would join next. */
        public final List<SeedBankEntry> unused = new ArrayList<>();
        /** Used entries (the "history"), newest first. */
        public final List<SeedBankEntry> used = new ArrayList<>();

        Stack(StackKey key) {
            this.key = key;
        }
    }

    /** Groups every saved entry into its stack, preserving insertion order for unused entries
     * (oldest first) and reversing it for the used/history list (newest first). Recomputed on
     * every call rather than cached - simplest correct option, and this only runs when a screen
     * opens/refreshes, not per tick. */
    public List<Stack> groupedStacks() {
        Map<StackKey, Stack> byKey = new LinkedHashMap<>();
        for (SeedBankEntry entry : entries) {
            StackKey key = new StackKey(entry.signature, entry.matchedCategories);
            Stack stack = byKey.computeIfAbsent(key, Stack::new);
            if (entry.used) {
                stack.used.add(0, entry);
            } else {
                stack.unused.add(entry);
            }
        }
        return new ArrayList<>(byKey.values());
    }
}
