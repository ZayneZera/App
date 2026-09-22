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

/* Vanilla ChunkRandom.setCarverSeed(worldSeed, chunkX, chunkZ), verified against decompiled
 * 1.16.1 source. Seeds a structure START's own Random (StructureStart.random) - used for
 * type/template/rotation/mirror/mossiness selection during structure placement search. This is a
 * COMPLETELY SEPARATE stream from setPopulationSeed/setDecoratorSeed (which seeds the later
 * chunk-decoration Random that piece.generate() draws the chest LootTableSeed from). chunkX/chunkZ
 * here are chunk coordinates (not block coordinates) - the chunk the structure search landed on. */
static inline uint64_t mc_setCarverSeed(uint64_t *seed, uint64_t worldSeed, int32_t chunkX, int32_t chunkZ) {
    setSeed(seed, worldSeed);
    uint64_t l = nextLong(seed);
    uint64_t m = nextLong(seed);
    uint64_t n = ((uint64_t)(int64_t)chunkX * l) ^ ((uint64_t)(int64_t)chunkZ * m) ^ worldSeed;
    setSeed(seed, n);
    return n;
}

/* Vanilla Structure.transformAround(pos, mirror, rotation, pivot), Y-component only ever passes
 * through unchanged (mirror/rotation only ever affect X/Z) - verified against decompiled 1.16.1
 * Structure.java. mirror: 0=NONE, 1=LEFT_RIGHT (unused by Ruined Portal), 2=FRONT_BACK.
 * rotation: 0=NONE, 1=CLOCKWISE_90, 2=CLOCKWISE_180, 3=COUNTERCLOCKWISE_90 (BlockRotation's
 * well-established vanilla declaration order since 1.13). */
static inline void mc_transformAround(int32_t x, int32_t y, int32_t z, int mirror, int rotation,
                                       int32_t pivotX, int32_t pivotZ, int32_t *outX, int32_t *outY, int32_t *outZ) {
    if (mirror == 2) x = -x;       /* FRONT_BACK */
    else if (mirror == 1) z = -z;  /* LEFT_RIGHT */

    int32_t rx, rz;
    switch (rotation) {
        case 3: /* COUNTERCLOCKWISE_90 */
            rx = pivotX - pivotZ + z;
            rz = pivotX + pivotZ - x;
            break;
        case 1: /* CLOCKWISE_90 */
            rx = pivotX + pivotZ - z;
            rz = pivotZ - pivotX + x;
            break;
        case 2: /* CLOCKWISE_180 */
            rx = pivotX + pivotX - x;
            rz = pivotZ + pivotZ - z;
            break;
        default: /* NONE */
            rx = x;
            rz = z;
            break;
    }
    *outX = rx;
    *outY = y;
    *outZ = rz;
}

/* Vanilla MathHelper.nextInt(random, min, max): inclusive integer range, used by both the loot
 * table "rolls" field (when given as a min/max object) and by the set_count function's count
 * range. Draws nothing if min >= max (matches UniformLootTableRange.next's short-circuit). */
static inline int32_t mc_nextIntRange(uint64_t *seed, int32_t min, int32_t max) {
    if (min >= max) return min;
    return nextInt(seed, max - min + 1) + min;
}

#endif
