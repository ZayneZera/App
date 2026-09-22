package com.zayne.seedfilter;

/**
 * One seed the seed bank's background search found and saved instead of joining. Plain data
 * class (Gson-serialized as-is, field names ARE the JSON keys) - see {@link SeedBank}.
 */
public class SeedBankEntry {
    public long seed;
    public int spawnX;
    public int spawnZ;
    public boolean cheats;
    public boolean creative;

    /** Bitwise-OR of engine.h's CATEGORY_* flags (see ExternalEngine) - which categories this
     * seed actually matched. Together with {@link #signature} this is the stack key: two entries
     * only stack together if both the search settings AND the matched set are identical. */
    public int matchedCategories;

    /** Only meaningful when (matchedCategories & ExternalEngine.CATEGORY_LOOTING_RP) != 0 - the
     * found portal's golden sword Looting level, 2 or 3. */
    public int lootingLevel;

    /** Canonical snapshot of the enabled categories' settings at search time (chunks, item
     * filters, bastion types) - see {@link SeedBank#buildSignature}. Deliberately excludes
     * cheats/creative/threadCount/orMode, which don't affect which seeds match. Used as (part of)
     * the stack-grouping key; {@link #settings} below carries the same information structured for
     * the seed bank table's columns instead of as one opaque string. */
    public String signature;

    /** The same search settings {@link #signature} encodes, kept structured for the seed bank
     * table's per-column display/sort instead of needing to re-parse the opaque signature string.
     * Never part of the stack-grouping key itself - signature already is. */
    public SearchSettings settings;

    public long timestampMillis;

    /** Set once the player has actually joined this seed via the bank - it stays saved (moves
     * into the stack's history view) rather than being deleted, so it's never re-suggested as a
     * fresh result but is still there to revisit. */
    public boolean used;

    /** Only meaningful when (matchedCategories & CATEGORY_RUINED_PORTAL) != 0 - mirrors
     * ExternalEngine.Result's rp* fields exactly so a saved entry can be turned back into a
     * Result and run through the normal createAndJoin()/RuinedPortalVerifier flow on join,
     * without re-running the search. The real frame-completability check (Crying Obsidian, gap
     * count) only ever happens there, once the player is actually in the generated world - the
     * background scan that found this entry only ever checked the RNG-only chest loot. */
    public boolean rpFound;
    public int rpPortalX, rpPortalZ;
    public int rpTemplateIndex, rpRotation, rpMirror;
    public int rpChestObsidian;

    /** Structured copy of the SeedFilterConfig fields that affect which seeds match - see
     * {@link #settings}. -1 for a chunk radius means that category was disabled for this search. */
    public static final class SearchSettings {
        public int villageChunks = -1;

        public int ruinedPortalChunks = -1;
        public boolean ruinedPortalLootingRequired;

        public int buriedTreasureChunks = -1;
        public int buriedTreasureMinTnt;
        public boolean buriedTreasureDiamondFilter;
        public boolean buriedTreasureIronFilter;
        public int buriedTreasureMinFish;

        public int bastionChunks = -1;
        public boolean bastionAllowBridge, bastionAllowHousing, bastionAllowStables, bastionAllowTreasure;

        public int fortressChunks = -1;

        public static SearchSettings capture(SeedFilterConfig cfg) {
            SearchSettings s = new SearchSettings();
            s.villageChunks = cfg.villageEnabled ? cfg.villageMaxChunks : -1;
            s.ruinedPortalChunks = cfg.ruinedPortalEnabled ? cfg.ruinedPortalMaxChunks : -1;
            s.ruinedPortalLootingRequired = cfg.ruinedPortalLootingSword;
            s.buriedTreasureChunks = cfg.buriedTreasureEnabled ? cfg.buriedTreasureMaxChunks : -1;
            s.buriedTreasureMinTnt = cfg.buriedTreasureMinTnt;
            s.buriedTreasureDiamondFilter = cfg.buriedTreasureDiamondFilter;
            s.buriedTreasureIronFilter = cfg.buriedTreasureIronFilter;
            s.buriedTreasureMinFish = cfg.buriedTreasureMinFish;
            s.bastionChunks = cfg.bastionEnabled ? cfg.bastionMaxNetherChunks : -1;
            s.bastionAllowBridge = cfg.bastionAllowBridge;
            s.bastionAllowHousing = cfg.bastionAllowHousing;
            s.bastionAllowStables = cfg.bastionAllowStables;
            s.bastionAllowTreasure = cfg.bastionAllowTreasure;
            s.fortressChunks = cfg.fortressEnabled ? cfg.fortressMaxNetherChunks : -1;
            return s;
        }
    }

    public ExternalEngine.Result toResult() {
        ExternalEngine.Result result = new ExternalEngine.Result(seed, spawnX, spawnZ, cheats, creative);
        result.matchedCategories = matchedCategories;
        result.lootingLevel = lootingLevel;
        if (rpFound) {
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

    public static SeedBankEntry fromResult(ExternalEngine.Result result, SeedFilterConfig cfg) {
        SeedBankEntry entry = new SeedBankEntry();
        entry.seed = result.seed;
        entry.spawnX = result.spawnX;
        entry.spawnZ = result.spawnZ;
        entry.cheats = cfg.enableCheats;
        entry.creative = cfg.creativeMode;
        entry.matchedCategories = result.matchedCategories;
        entry.lootingLevel = result.lootingLevel;
        entry.signature = SeedBank.buildSignature(cfg);
        entry.settings = SearchSettings.capture(cfg);
        entry.timestampMillis = System.currentTimeMillis();
        entry.used = false;
        entry.rpFound = result.rpFound;
        entry.rpPortalX = result.rpPortalX;
        entry.rpPortalZ = result.rpPortalZ;
        entry.rpTemplateIndex = result.rpTemplateIndex;
        entry.rpRotation = result.rpRotation;
        entry.rpMirror = result.rpMirror;
        entry.rpChestObsidian = result.rpChestObsidian;
        return entry;
    }
}
