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

    /** Canonical snapshot of the enabled categories' settings at search time (chunks, item
     * filters, bastion types) - see {@link SeedBank#buildSignature}. Deliberately excludes
     * cheats/creative/threadCount/orMode, which don't affect which seeds match. */
    public String signature;

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

    public ExternalEngine.Result toResult() {
        ExternalEngine.Result result = new ExternalEngine.Result(seed, spawnX, spawnZ, cheats, creative);
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

    public static SeedBankEntry fromResult(ExternalEngine.Result result, String signature, boolean cheats, boolean creative) {
        SeedBankEntry entry = new SeedBankEntry();
        entry.seed = result.seed;
        entry.spawnX = result.spawnX;
        entry.spawnZ = result.spawnZ;
        entry.cheats = cheats;
        entry.creative = creative;
        entry.matchedCategories = result.matchedCategories;
        entry.signature = signature;
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
