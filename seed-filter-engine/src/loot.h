#ifndef SEEDFILTER_LOOT_H_
#define SEEDFILTER_LOOT_H_

#include <stdint.h>

typedef struct {
    int tnt;
    int diamond;
    int iron;
    int fish; /* cooked_cod + cooked_salmon combined */
} BuriedTreasureLoot;

/* Resolves a buried treasure chest's contents (TNT/diamond/iron totals across the whole chest)
 * for the treasure generated in the given chunk on the given world seed.
 *
 * structureIndex/step are baked in: Buried_Treasure is registered at index 1 within
 * GenerationStep.Feature.UNDERGROUND_STRUCTURES (ordinal 3), verified against decompiled 1.16.1
 * StructureFeature.java and GenerationStep.java.
 *
 * Assumes the chest's LootTableSeed is the FIRST long drawn from the decorator-seeded random -
 * BuriedTreasureGenerator.Piece places a single chest with no NBT template/other block entities,
 * so there is nothing else for that piece to draw from the shared random before reaching it. This
 * hasn't been confirmed against BuriedTreasureGenerator.java's actual source, only inferred from
 * BuriedTreasureFeature.Start.init() (which only ever adds one child piece) - flagged for
 * empirical validation against a real seed. */
void loot_buriedTreasure(uint64_t worldSeed, int32_t chunkX, int32_t chunkZ, BuriedTreasureLoot *out);

#endif
