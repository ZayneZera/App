#include "loot.h"
#include "mc_random.h"

/* Weighted-entry pick: mirrors vanilla LootPool.choose() - draws nextInt(totalWeight), then
 * walks entries subtracting each one's weight until the remainder goes negative. Always draws,
 * even when there's only one entry (totalWeight == its own weight) - Java's nextInt(1) still
 * consumes a sample, so skipping the draw here would desync every later pool. */
static int32_t pick_entry(uint64_t *seed, const int32_t *weights, int32_t count) {
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

    uint64_t seed;
    uint64_t populationSeed = mc_setPopulationSeed(&seed, worldSeed, chunkX * 16, chunkZ * 16);
    /* Buried_Treasure: index 1 within GenerationStep.Feature.UNDERGROUND_STRUCTURES (ordinal 3). */
    mc_setDecoratorSeed(&seed, populationSeed, 1, 3);
    uint64_t lootTableSeed = nextLong(&seed);

    uint64_t s;
    setSeed(&s, lootTableSeed);

    /* Pool 1: rolls=1 (fixed, no draw), single entry (heart_of_the_sea) - still draws for the
     * entry pick even though there's only one choice. Nothing we track here. */
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

    /* Pool 5: rolls=2 (fixed, no draw). Entries: cooked_cod(2-4), cooked_salmon(2-4), both w1. */
    {
        int32_t weights[] = {1, 1};
        for (int32_t i = 0; i < 2; i++) {
            pick_entry(&s, weights, 2);
            mc_nextIntRange(&s, 2, 4);
        }
    }
}
