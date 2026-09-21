#include "engine.h"

#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "finders.h"
#include "generator.h"

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

int engine_check_seed(uint64_t seed, const FilterConfig *cfg, FilterResult *out) {
    Generator gOverworld;
    setupGenerator(&gOverworld, MC, 0);
    applySeed(&gOverworld, DIM_OVERWORLD, seed);

    Pos spawn = getSpawn(&gOverworld);
    int spawnChunkBlockX = spawn.x, spawnChunkBlockZ = spawn.z;

    if (cfg->villageEnabled) {
        if (!find_structure_within(Village, &gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg->villageMaxChunks, NULL)) {
            return 0;
        }
    }

    if (cfg->ruinedPortalEnabled) {
        if (!find_structure_within(Ruined_Portal, &gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg->ruinedPortalMaxChunks, NULL)) {
            return 0;
        }
    }

    if (cfg->buriedTreasureEnabled) {
        if (!find_structure_within(Treasure, &gOverworld, seed, spawnChunkBlockX, spawnChunkBlockZ, cfg->buriedTreasureMaxChunks, NULL)) {
            return 0;
        }
    }

    if (cfg->bastionEnabled || cfg->fortressEnabled) {
        /* Nether reference point: overworld spawn divided by 8 (overworld<->nether ratio). */
        int netherX = spawnChunkBlockX / 8;
        int netherZ = spawnChunkBlockZ / 8;

        Generator gNether;
        setupGenerator(&gNether, MC, 0);
        applySeed(&gNether, DIM_NETHER, seed);

        if (cfg->bastionEnabled) {
            if (!check_bastion(&gNether, seed, netherX, netherZ, cfg)) {
                return 0;
            }
        }

        if (cfg->fortressEnabled) {
            if (!find_structure_within(Fortress, &gNether, seed, netherX, netherZ, cfg->fortressMaxNetherChunks, NULL)) {
                return 0;
            }
        }
    }

    if (out) {
        out->seed = seed;
        out->spawnX = spawn.x;
        out->spawnZ = spawn.z;
        out->enableCheats = cfg->enableCheats;
    }
    return 1;
}
