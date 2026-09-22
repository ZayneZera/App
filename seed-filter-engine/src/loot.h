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

typedef struct {
    int obsidian;
    int flintAndSteel;
    int fireCharge;
    int goldenAxe;
} RuinedPortalLoot;

/* Resolves a ruined portal chest's contents (obsidian count, flint-and-steel/fire-charge presence,
 * golden axe presence) for the portal generated in the given chunk on the given world seed.
 *
 * structureIndex/step are baked in: Ruined_Portal is registered at index 5 within
 * GenerationStep.Feature.SURFACE_STRUCTURES (ordinal 4) - verified against decompiled 1.16.1
 * StructureFeature.java (registration order: Pillager_Outpost, Mansion, Jungle_Pyramid,
 * Desert_Pyramid, Igloo, Ruined_Portal=index 5, ...) and Biome.generateFeatureStep(), which assigns
 * each structure an index by iterating Registry.STRUCTURE_FEATURE in registration order and
 * counting only the structures that share the current step - the same mechanism already verified
 * for Buried Treasure against a real seed.
 *
 * The chest's LootTableSeed draw happens inside Structure.place() while placing the template's
 * blocks in order, and is independent of the structure-start ("carver seed") Random that picks the
 * portal's template/rotation/mirror/mossiness - so this loot content is exact regardless of that
 * (terrain-height-dependent) selection, which is NOT modeled here. */
void loot_ruinedPortal(uint64_t worldSeed, int32_t chunkX, int32_t chunkZ, RuinedPortalLoot *out);

#endif
