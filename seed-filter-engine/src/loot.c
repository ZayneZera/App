#include "loot.h"
#include "mc_random.h"

/* Weighted-entry pick: mirrors vanilla LootPool.supplyOnce() exactly, verified against
 * decompiled 1.16.1 source. CRITICAL: when there's only one candidate entry, vanilla returns it
 * directly WITHOUT drawing from random at all ("if (i == 1) { ...generateLoot... } else {
 * ...random.nextInt(totalWeight)... }") - only calls nextInt() when there are 2+ entries to
 * choose between. Drawing unconditionally here (as an earlier version of this function did)
 * desyncs the whole rest of the pool sequence for any pool with a single entry (e.g. buried
 * treasure's heart_of_the_sea pool) - confirmed against a real seed's actual chest contents. */
static int32_t pick_entry(uint64_t *seed, const int32_t *weights, int32_t count) {
    if (count == 1) return 0;
    int32_t total = 0;
    for (int32_t i = 0; i < count; i++) total += weights[i];
    int32_t r = nextInt(seed, total);
    for (int32_t i = 0; i < count; i++) {
        r -= weights[i];
        if (r < 0) return i;
    }
    return count - 1;
}

void loot_buriedTreasure(uint64_t worldSeed, int32_t chunkX, int32_t chunkZ, BuriedTreasureLoot *out) {
    out->tnt = 0;
    out->diamond = 0;
    out->iron = 0;
    out->fish = 0;

    uint64_t seed;
    uint64_t populationSeed = mc_setPopulationSeed(&seed, worldSeed, chunkX * 16, chunkZ * 16);
    /* Buried_Treasure: index 1 within GenerationStep.Feature.UNDERGROUND_STRUCTURES (ordinal 3). */
    mc_setDecoratorSeed(&seed, populationSeed, 1, 3);
    uint64_t lootTableSeed = nextLong(&seed);

    uint64_t s;
    setSeed(&s, lootTableSeed);

    /* Pool 1: rolls=1 (fixed, no draw), single entry (heart_of_the_sea) - pick_entry(count=1)
     * also draws nothing, matching vanilla exactly. Nothing we track here. */
    {
        int32_t weights[] = {1};
        pick_entry(&s, weights, 1);
    }

    /* Pool 2: rolls uniform(5,8). Entries: iron_ingot(w20,1-4), gold_ingot(w10,1-4), tnt(w5,1-2). */
    {
        int32_t rolls = mc_nextIntRange(&s, 5, 8);
        int32_t weights[] = {20, 10, 5};
        for (int32_t i = 0; i < rolls; i++) {
            int32_t pick = pick_entry(&s, weights, 3);
            if (pick == 0) out->iron += mc_nextIntRange(&s, 1, 4);
            else if (pick == 1) mc_nextIntRange(&s, 1, 4); /* gold_ingot, untracked but still draws */
            else out->tnt += mc_nextIntRange(&s, 1, 2);
        }
    }

    /* Pool 3: rolls uniform(1,3). Entries: emerald(w5,4-8), diamond(w5,1-2), prismarine_crystals(w5,1-5). */
    {
        int32_t rolls = mc_nextIntRange(&s, 1, 3);
        int32_t weights[] = {5, 5, 5};
        for (int32_t i = 0; i < rolls; i++) {
            int32_t pick = pick_entry(&s, weights, 3);
            if (pick == 0) mc_nextIntRange(&s, 4, 8); /* emerald, untracked but still draws */
            else if (pick == 1) out->diamond += mc_nextIntRange(&s, 1, 2);
            else mc_nextIntRange(&s, 1, 5); /* prismarine_crystals, untracked but still draws */
        }
    }

    /* Pool 4: rolls uniform(0,1). Entries: leather_chestplate(w1), iron_sword(w1) - neither has
     * a set_count function, so no count draw regardless of which is picked. */
    {
        int32_t rolls = mc_nextIntRange(&s, 0, 1);
        int32_t weights[] = {1, 1};
        for (int32_t i = 0; i < rolls; i++) {
            pick_entry(&s, weights, 2);
        }
    }

    /* Pool 5: rolls=2 (fixed, no draw). Entries: cooked_cod(2-4), cooked_salmon(2-4), both w1.
     * Always exactly 2 rolls at 2-4 each regardless of which fish is picked, so total fish is
     * always in [4,8] - every buried treasure chest has at least 4 fish, guaranteed. */
    {
        int32_t weights[] = {1, 1};
        for (int32_t i = 0; i < 2; i++) {
            pick_entry(&s, weights, 2);
            out->fish += mc_nextIntRange(&s, 2, 4);
        }
    }
}
