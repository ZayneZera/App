#include "engine.h"

#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "finders.h"
#include "generator.h"
#include "loot.h"
#include "frame.h"

#define MC MC_1_16_1

/* Finds any viable position of structureType within maxChunks (real circular distance) of
 * (centerBlockX, centerBlockZ). Returns 1 and fills *outPos on success. Mirrors the box-scan
 * pattern cubiomes' own tests.c demonstrates for structure searches. */
static int find_structure_within(int structureType, Generator *g, uint64_t seed, int centerBlockX, int centerBlockZ,
                                  int maxChunks, Pos *outPos) {
    StructureConfig sconf;
    if (!getStructureConfig(structureType, MC, &sconf)) {
        return 0;
    }

    double blocksPerRegion = sconf.regionSize * 16.0;
    int maxBlocks = maxChunks * 16;
    double x0 = centerBlockX - maxBlocks, x1 = centerBlockX + maxBlocks;
    double z0 = centerBlockZ - maxBlocks, z1 = centerBlockZ + maxBlocks;
    int rx0 = (int) floor(x0 / blocksPerRegion), rx1 = (int) ceil(x1 / blocksPerRegion);
    int rz0 = (int) floor(z0 / blocksPerRegion), rz1 = (int) ceil(z1 / blocksPerRegion);

    for (int j = rz0; j <= rz1; j++) {
        for (int i = rx0; i <= rx1; i++) {
            Pos pos;
            if (!getStructurePos(structureType, MC, seed, i, j, &pos)) {
                continue;
            }
            double dx = pos.x - centerBlockX, dz = pos.z - centerBlockZ;
            double distChunks = sqrt(dx * dx + dz * dz) / 16.0;
            if (distChunks > maxChunks) {
                continue;
            }
            if (!isViableStructurePos(structureType, g, pos.x, pos.z, 0)) {
                continue;
            }
            if (outPos) *outPos = pos;
            return 1;
        }
    }
    return 0;
}

static int bastion_type_allowed(int start, const FilterConfig *cfg) {
    switch (start) {
        case 0: return cfg->bastionAllowHousing;
        case 1: return cfg->bastionAllowStables;
        case 2: return cfg->bastionAllowTreasure;
        case 3: return cfg->bastionAllowBridge;
        default: return 0;
    }
}

static int check_bastion(Generator *gNether, uint64_t seed, int centerBlockX, int centerBlockZ, const FilterConfig *cfg) {
    StructureConfig sconf;
    if (!getStructureConfig(Bastion, MC, &sconf)) return 0;

    double blocksPerRegion = sconf.regionSize * 16.0;
    int maxBlocks = cfg->bastionMaxNetherChunks * 16;
    double x0 = centerBlockX - maxBlocks, x1 = centerBlockX + maxBlocks;
    double z0 = centerBlockZ - maxBlocks, z1 = centerBlockZ + maxBlocks;
    int rx0 = (int) floor(x0 / blocksPerRegion), rx1 = (int) ceil(x1 / blocksPerRegion);
    int rz0 = (int) floor(z0 / blocksPerRegion), rz1 = (int) ceil(z1 / blocksPerRegion);

    for (int j = rz0; j <= rz1; j++) {
        for (int i = rx0; i <= rx1; i++) {
            Pos pos;
            if (!getStructurePos(Bastion, MC, seed, i, j, &pos)) continue;
            double dx = pos.x - centerBlockX, dz = pos.z - centerBlockZ;
            double distChunks = sqrt(dx * dx + dz * dz) / 16.0;
            if (distChunks > cfg->bastionMaxNetherChunks) continue;
            if (!isViableStructurePos(Bastion, gNether, pos.x, pos.z, 0)) continue;

            int biomeId = getBiomeAt(gNether, 4, pos.x >> 2, 0, pos.z >> 2);
            StructureVariant sv;
            getVariant(&sv, Bastion, MC, seed, pos.x, pos.z, biomeId);
            if (bastion_type_allowed(sv.start, cfg)) {
                return 1;
            }
        }
    }
    return 0;
}

#define BUMP(field) do { if (stats) __sync_fetch_and_add(&stats->field, 1); } while (0)

static int category_village(Generator *gOverworld, uint64_t seed, int spawnX, int spawnZ,
                             const FilterConfig *cfg, ScanStats *stats) {
    BUMP(reachedVillage);
    if (!find_structure_within(Village, gOverworld, seed, spawnX, spawnZ, cfg->villageMaxChunks, NULL)) {
        return 0;
    }
    BUMP(passedVillage);
    return 1;
}

static int category_ruined_portal(Generator *gOverworld, uint64_t seed, int spawnX, int spawnZ,
                                   const FilterConfig *cfg, FilterResult *out, ScanStats *stats) {
    BUMP(reachedRuinedPortal);
    Pos portalPos;
    if (!find_structure_within(Ruined_Portal, gOverworld, seed, spawnX, spawnZ, cfg->ruinedPortalMaxChunks, &portalPos)) {
        return 0;
    }
    /* Fixed requirements (always on while Ruined Portal is enabled, no separate toggle):
     * chest needs a golden axe, and either a flint and steel or a fire charge. */
    RuinedPortalLoot rpLoot;
    loot_ruinedPortal(seed, portalPos.x >> 4, portalPos.z >> 4, &rpLoot);
    if (rpLoot.goldenAxe < 1) return 0;
    if (rpLoot.flintAndSteel < 1 && rpLoot.fireCharge < 1) return 0;
    if (cfg->ruinedPortalRequireLootingSword && rpLoot.swordLootingLevel < 2) return 0;

    if (cfg->ruinedPortalFrameCheck) {
        int biomeID = getBiomeAt(gOverworld, 4, portalPos.x >> 2, 0, portalPos.z >> 2);
        int templateIndex, rotation, mirror;
        if (!rp_determinePlacement(seed, biomeID, portalPos.x, portalPos.z, &templateIndex, &rotation, &mirror)) {
            return 0;
        }
        if (out) {
            out->rpFound = 1;
            out->rpPortalX = portalPos.x;
            out->rpPortalZ = portalPos.z;
            out->rpTemplateIndex = templateIndex;
            out->rpRotation = rotation;
            out->rpMirror = mirror;
            out->rpChestObsidian = rpLoot.obsidian;
        }
    }
    BUMP(passedRuinedPortal);
    return 1;
}

static int category_treasure(Generator *gOverworld, uint64_t seed, int spawnX, int spawnZ,
                              const FilterConfig *cfg, ScanStats *stats) {
    BUMP(reachedTreasure);
    Pos treasurePos;
    if (!find_structure_within(Treasure, gOverworld, seed, spawnX, spawnZ, cfg->buriedTreasureMaxChunks, &treasurePos)) {
        return 0;
    }
    if (cfg->buriedTreasureMinTnt > 0 || cfg->buriedTreasureDiamondFilter || cfg->buriedTreasureIronFilter ||
        cfg->buriedTreasureMinFish > 4) {
        BuriedTreasureLoot loot;
        loot_buriedTreasure(seed, treasurePos.x >> 4, treasurePos.z >> 4, &loot);
        if (loot.tnt < cfg->buriedTreasureMinTnt) return 0;
        if (cfg->buriedTreasureDiamondFilter && loot.diamond < 3) return 0;
        if (cfg->buriedTreasureIronFilter) {
            int minIron = cfg->buriedTreasureDiamondFilter ? 7 : 10;
            if (loot.iron < minIron) return 0;
        }
        if (loot.fish < cfg->buriedTreasureMinFish) return 0;
    }
    BUMP(passedTreasure);
    return 1;
}

static int category_bastion(Generator *gNether, uint64_t seed, int netherX, int netherZ,
                             const FilterConfig *cfg, ScanStats *stats) {
    BUMP(reachedBastion);
    if (!check_bastion(gNether, seed, netherX, netherZ, cfg)) {
        return 0;
    }
    BUMP(passedBastion);
    return 1;
}

static int category_fortress(Generator *gNether, uint64_t seed, int netherX, int netherZ,
                              const FilterConfig *cfg, ScanStats *stats) {
    BUMP(reachedFortress);
    if (!find_structure_within(Fortress, gNether, seed, netherX, netherZ, cfg->fortressMaxNetherChunks, NULL)) {
        return 0;
    }
    BUMP(passedFortress);
    return 1;
}

int engine_check_seed(uint64_t seed, const FilterConfig *cfg, FilterResult *out, ScanStats *stats) {
    if (out) {
        out->rpFound = 0;
        out->matchedCategories = 0;
    }

    Generator gOverworld;
    setupGenerator(&gOverworld, MC, 0);
    applySeed(&gOverworld, DIM_OVERWORLD, seed);

    Pos spawn = getSpawn(&gOverworld);
    int spawnChunkBlockX = spawn.x, spawnChunkBlockZ = spawn.z;
    /* Nether reference point: overworld spawn divided by 8 (overworld<->nether ratio). */
    int netherX = spawnChunkBlockX / 8;
    int netherZ = spawnChunkBlockZ / 8;
    Generator gNether;

    if (!cfg->orMode) {
        /* Original AND semantics, unchanged: early-exit the instant any enabled category fails,
         * so a rare/selective category checked early skips the cost of later ones entirely - in
         * particular the Nether generator setup below stays lazy, paid only once every cheaper
         * overworld category already passed, never for a seed that was going to be rejected
         * anyway. */
        if (cfg->villageEnabled && !category_village(&gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg, stats)) return 0;
        if (cfg->ruinedPortalEnabled && !category_ruined_portal(&gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg, out, stats)) return 0;
        if (cfg->buriedTreasureEnabled && !category_treasure(&gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg, stats)) return 0;

        if (cfg->bastionEnabled || cfg->fortressEnabled) {
            setupGenerator(&gNether, MC, 0);
            applySeed(&gNether, DIM_NETHER, seed);
            if (cfg->bastionEnabled && !category_bastion(&gNether, seed, netherX, netherZ, cfg, stats)) return 0;
            if (cfg->fortressEnabled && !category_fortress(&gNether, seed, netherX, netherZ, cfg, stats)) return 0;
        }

        if (out) {
            int matched = 0;
            if (cfg->villageEnabled) matched |= CATEGORY_VILLAGE;
            if (cfg->ruinedPortalEnabled) matched |= CATEGORY_RUINED_PORTAL;
            if (cfg->buriedTreasureEnabled) matched |= CATEGORY_TREASURE;
            if (cfg->bastionEnabled) matched |= CATEGORY_BASTION;
            if (cfg->fortressEnabled) matched |= CATEGORY_FORTRESS;
            out->matchedCategories = matched;
        }
    } else {
        /* OR semantics: every enabled category is evaluated regardless of the others' outcome
         * (no early exit - we need to know the full matched set, not just whether ANY passed),
         * and a match is ANY non-empty result. More than one bit set means this seed satisfies
         * multiple independently - the mod's seed bank tags that "OP". */
        int matched = 0;
        if (cfg->villageEnabled && category_village(&gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg, stats)) matched |= CATEGORY_VILLAGE;
        if (cfg->ruinedPortalEnabled && category_ruined_portal(&gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg, out, stats)) matched |= CATEGORY_RUINED_PORTAL;
        if (cfg->buriedTreasureEnabled && category_treasure(&gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg, stats)) matched |= CATEGORY_TREASURE;

        if (cfg->bastionEnabled || cfg->fortressEnabled) {
            setupGenerator(&gNether, MC, 0);
            applySeed(&gNether, DIM_NETHER, seed);
            if (cfg->bastionEnabled && category_bastion(&gNether, seed, netherX, netherZ, cfg, stats)) matched |= CATEGORY_BASTION;
            if (cfg->fortressEnabled && category_fortress(&gNether, seed, netherX, netherZ, cfg, stats)) matched |= CATEGORY_FORTRESS;
        }

        if (matched == 0) return 0;
        if (out) out->matchedCategories = matched;
    }

    if (out) {
        out->seed = seed;
        out->spawnX = spawn.x;
        out->spawnZ = spawn.z;
        out->enableCheats = cfg->enableCheats;
        out->creativeMode = cfg->creativeMode;
    }
    return 1;
}
