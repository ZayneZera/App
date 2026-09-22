#ifndef SEEDFILTER_MC_RANDOM_H_
#define SEEDFILTER_MC_RANDOM_H_

#include <stdint.h>
#include "../third_party/cubiomes/rng.h"

/* Vanilla ChunkRandom.setPopulationSeed(worldSeed, blockX, blockZ), verified against decompiled
 * 1.16.1 source (net.minecraft.world.gen.ChunkRandom). Seeds the randomizer used to place
 * decoration-phase features (including structure block generation) for one chunk. blockX/blockZ
 * are the chunk's negative-most block coordinates (chunkX*16, chunkZ*16), NOT chunk coordinates. */
static inline uint64_t mc_setPopulationSeed(uint64_t *seed, uint64_t worldSeed, int32_t blockX, int32_t blockZ) {
    setSeed(seed, worldSeed);
    uint64_t l = nextLong(seed) | 1ULL;
    uint64_t m = nextLong(seed) | 1ULL;
    uint64_t n = ((uint64_t)(int64_t)blockX * l + (uint64_t)(int64_t)blockZ * m) ^ worldSeed;
    setSeed(seed, n);
    return n;
}

/* Vanilla ChunkRandom.setDecoratorSeed(populationSeed, index, step). index is this structure's
 * ordinal among all StructureFeatures sharing the same GenerationStep.Feature step, counted in
 * Registry.STRUCTURE_FEATURE registration order. step is that GenerationStep.Feature's ordinal.
 * This is the Random ultimately passed into StructureStart.generateStructure() / piece.generate(),
 * and therefore the stream that Structure.place() draws chest LootTableSeed values from. */
static inline uint64_t mc_setDecoratorSeed(uint64_t *seed, uint64_t populationSeed, int32_t index, int32_t step) {
    uint64_t l = populationSeed + (uint64_t)(int64_t)index + (uint64_t)(int64_t)(10000 * step);
    setSeed(seed, l);
    return l;
}

/* Vanilla StructurePlacementData.getRandom(pos) fallback branch: new Random(MathHelper.hashCode(pos)).
 * Used by BlockRotStructureProcessor (block_age) for per-block degradation rolls - each block's
 * survive/remove decision is an INDEPENDENT draw seeded purely from its own world position, with
 * no dependency on world seed or draw order. Verified against decompiled 1.16.1 MathHelper.hashCode
 * (it returns a Java `long`, not `int` - the intermediate x*3129871 multiply is 32-bit though). */
static inline uint64_t mc_hashCodeSeed(uint64_t *seed, int32_t x, int32_t y, int32_t z) {
    int32_t x3 = x * 3129871;
    int64_t l = (int64_t)x3 ^ ((int64_t)z * 116129781LL) ^ (int64_t)y;
    l = l * l * 42317861LL + l * 11LL;
    l = l >> 16;
    setSeed(seed, (uint64_t)l);
    return (uint64_t)l;
}

/* Vanilla MathHelper.nextInt(random, min, max): inclusive integer range, used by both the loot
 * table "rolls" field (when given as a min/max object) and by the set_count function's count
 * range. Draws nothing if min >= max (matches UniformLootTableRange.next's short-circuit). */
static inline int32_t mc_nextIntRange(uint64_t *seed, int32_t min, int32_t max) {
    if (min >= max) return min;
    return nextInt(seed, max - min + 1) + min;
}

#endif
