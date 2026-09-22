#include "frame.h"
#include "mc_random.h"
#include "biomes.h"
#include "biomenoise.h"

typedef struct { int8_t x, y, z, isObsidian; } RPFrameCell;

/* Per-template border-minus-corner frame cells, in the template's own local coordinate space
 * (origin at the template's (0,0,0) corner, as stored in the structure NBT). Extracted by parsing
 * the actual ruined_portal .nbt files from the 1.16.1 client jar and locating each template's
 * obsidian/air portal-frame rectangle. isObsidian=1 means the template places obsidian there
 * (subject to BlockAgeStructureProcessor's fixed 15% Crying Obsidian roll); isObsidian=0 means the
 * template places air there. */

static const RPFrameCell RP_CELLS_PORTAL_1[] = {
    {3,2,2,1}, {3,2,3,1}, {3,3,1,1}, {3,3,4,1}, {3,4,1,1},
    {3,4,4,0}, {3,5,1,1}, {3,5,4,0}, {3,6,2,1}, {3,6,3,1},
};
static const RPFrameCell RP_CELLS_PORTAL_2[] = {
    {5,4,3,0}, {5,4,4,0}, {5,5,2,1}, {5,5,5,0}, {5,6,2,1},
    {5,6,5,0}, {5,7,2,1}, {5,7,5,1}, {5,8,3,1}, {5,8,4,1},
};
static const RPFrameCell RP_CELLS_PORTAL_3[] = {
    {4,3,3,1}, {4,3,4,1}, {4,4,2,0}, {4,4,5,1}, {4,5,2,0},
    {4,5,5,1}, {4,6,2,0}, {4,6,5,1}, {4,7,3,0}, {4,7,4,1},
};
static const RPFrameCell RP_CELLS_PORTAL_4[] = {
    {4,3,3,1}, {4,3,4,1}, {4,4,2,1}, {4,4,5,1}, {4,5,2,1},
    {4,5,5,1}, {4,6,2,1}, {4,6,5,0}, {4,7,3,0}, {4,7,4,0},
};
static const RPFrameCell RP_CELLS_PORTAL_6[] = {
    {2,1,1,1}, {2,1,2,1}, {2,1,3,1}, {2,2,0,1}, {2,2,4,1},
    {2,3,0,1}, {2,3,4,1}, {2,4,0,1}, {2,4,4,1}, {2,5,1,1},
    {2,5,2,0}, {2,5,3,1},
};
static const RPFrameCell RP_CELLS_PORTAL_8[] = {
    {5,3,3,1}, {5,3,4,1}, {5,4,2,1}, {5,4,5,0}, {5,5,2,1},
    {5,5,5,0}, {5,6,2,1}, {5,6,5,0}, {5,7,3,0}, {5,7,4,0},
};
static const RPFrameCell RP_CELLS_PORTAL_9[] = {
    {4,1,4,1}, {4,1,5,1}, {4,2,3,1}, {4,2,6,1}, {4,3,3,0},
    {4,3,6,1}, {4,4,3,0}, {4,4,6,1}, {4,5,4,1}, {4,5,5,1},
};

typedef struct {
    int commonIndex; /* index into RuinedPortalFeature.COMMON_PORTAL_STRUCTURE_IDS */
    int32_t sizeX, sizeY, sizeZ;
    const RPFrameCell *cells;
    int cellCount;
} RPTemplate;

#define TPL(idx, sx, sy, sz, arr) {idx, sx, sy, sz, arr, (int)(sizeof(arr) / sizeof(arr[0]))}
static const RPTemplate RP_TEMPLATES[] = {
    TPL(0, 6, 10, 6, RP_CELLS_PORTAL_1),
    TPL(1, 9, 12, 9, RP_CELLS_PORTAL_2),
    TPL(2, 8, 9, 9, RP_CELLS_PORTAL_3),
    TPL(3, 8, 9, 9, RP_CELLS_PORTAL_4),
    /* index 4 (portal_5) and 6 (portal_7) and 9 (portal_10) intentionally absent - see frame.h */
    TPL(5, 5, 7, 7, RP_CELLS_PORTAL_6),
    TPL(7, 14, 9, 9, RP_CELLS_PORTAL_8),
    TPL(8, 10, 8, 9, RP_CELLS_PORTAL_9),
};
#define RP_TEMPLATE_COUNT (int)(sizeof(RP_TEMPLATES) / sizeof(RP_TEMPLATES[0]))

static const RPTemplate *findTemplate(int commonIndex) {
    for (int i = 0; i < RP_TEMPLATE_COUNT; i++) {
        if (RP_TEMPLATES[i].commonIndex == commonIndex) return &RP_TEMPLATES[i];
    }
    return NULL;
}

enum { RPTYPE_STANDARD, RPTYPE_DESERT, RPTYPE_JUNGLE, RPTYPE_SWAMP, RPTYPE_MOUNTAIN, RPTYPE_OCEAN };
enum { VP_LAND_SURFACE, VP_OCEAN_FLOOR, VP_PARTLY_BURIED, VP_IN_MOUNTAIN, VP_UNDERGROUND };

static int classifyPortalType(int biomeID) {
    switch (biomeID) {
        case desert: case desert_hills: case desert_lakes:
            return RPTYPE_DESERT;
        case jungle: case jungle_hills: case jungle_edge:
        case modified_jungle: case modified_jungle_edge:
        case bamboo_jungle: case bamboo_jungle_hills:
            return RPTYPE_JUNGLE;
        case swamp: case swamp_hills:
            return RPTYPE_SWAMP;
        case mountains: case wooded_mountains: case gravelly_mountains:
        case modified_gravelly_mountains: case mountain_edge:
            return RPTYPE_MOUNTAIN;
        default:
            return isOceanic(biomeID) ? RPTYPE_OCEAN : RPTYPE_STANDARD;
    }
}

int rp_checkFrame(const Generator *gOverworld, uint64_t worldSeed, int biomeID, int32_t portalChunkX, int32_t portalChunkZ, int32_t *outAirCount) {
    uint64_t rnd;
    mc_setCarverSeed(&rnd, worldSeed, portalChunkX, portalChunkZ);

    int portalType = classifyPortalType(biomeID);
    int airPocket;
    int verticalPlacement;
    switch (portalType) {
        case RPTYPE_DESERT:
            airPocket = 0;
            verticalPlacement = VP_PARTLY_BURIED;
            break;
        case RPTYPE_JUNGLE:
            airPocket = nextFloat(&rnd) < 0.5F;
            verticalPlacement = VP_LAND_SURFACE;
            break;
        case RPTYPE_SWAMP:
            airPocket = 0;
            verticalPlacement = VP_OCEAN_FLOOR;
            break;
        case RPTYPE_MOUNTAIN: {
            int bl = nextFloat(&rnd) < 0.5F;
            verticalPlacement = bl ? VP_IN_MOUNTAIN : VP_LAND_SURFACE;
            airPocket = bl || (nextFloat(&rnd) < 0.5F);
            break;
        }
        case RPTYPE_OCEAN:
            airPocket = 0;
            verticalPlacement = VP_OCEAN_FLOOR;
            break;
        default: { /* STANDARD */
            int bl = nextFloat(&rnd) < 0.5F;
            verticalPlacement = bl ? VP_UNDERGROUND : VP_LAND_SURFACE;
            airPocket = bl || (nextFloat(&rnd) < 0.5F);
            break;
        }
    }

    /* Requested/accepted restriction: only accept portals actually exposed on land at the
     * surface, never underground/in-mountain/partly-buried/underwater. This both matches what
     * the user wants (a portal they can actually walk up to right away) and sidesteps the much
     * larger height-approximation error those placements would otherwise have (their Y range
     * depends on a random draw over a potentially huge depth span, whereas ON_LAND_SURFACE's
     * height comes directly from the (approximated) terrain height with no extra randomness). */
    if (verticalPlacement != VP_LAND_SURFACE) {
        return 0;
    }

    if (nextFloat(&rnd) < 0.05F) {
        nextInt(&rnd, 3); /* rare (giant) template roll - not analyzed, reject */
        return 0;
    }
    int commonIndex = nextInt(&rnd, 10);

    const RPTemplate *tpl = findTemplate(commonIndex);
    if (!tpl) return 0; /* portal_5/7/10 - no usable frame geometry, reject */

    int rotation = nextInt(&rnd, 4);                       /* 0=NONE,1=CW90,2=CW180,3=CCW90 */
    int mirror = (nextFloat(&rnd) < 0.5F) ? 0 : 2;          /* 0=NONE, 2=FRONT_BACK */

    int32_t pivotX = tpl->sizeX / 2;
    int32_t pivotZ = tpl->sizeZ / 2;
    int32_t chunkCenterX = portalChunkX * 16 + 8;
    int32_t chunkCenterZ = portalChunkZ * 16 + 8;

    /* Vanilla scans downward from the initial terrain height and stops at the first Y where at
     * least 3 of the structure box's 4 corners have solid ground (a heightmap-predicate check we
     * can't replicate without real block data). Since that predicate is true for a corner at every
     * Y at or below its own terrain surface and false above it, "at least 3 of 4 corners satisfy"
     * is equivalent to taking the 3rd-highest (2nd-lowest) of the 4 corners' own terrain heights -
     * so we approximate each corner's height via mapApproxHeight and use that order statistic,
     * rather than a single center-point sample (which is what caused visibly wrong placements). */
    SurfaceNoise sn;
    initSurfaceNoise(&sn, DIM_OVERWORLD, worldSeed);

    int32_t localCornersX[4] = {0, tpl->sizeX - 1, 0, tpl->sizeX - 1};
    int32_t localCornersZ[4] = {0, 0, tpl->sizeZ - 1, tpl->sizeZ - 1};
    int32_t cornerHeights[4];
    for (int i = 0; i < 4; i++) {
        int32_t tx, ty, tz;
        mc_transformAround(localCornersX[i], 0, localCornersZ[i], mirror, rotation, pivotX, pivotZ, &tx, &ty, &tz);
        int32_t wx = tx + chunkCenterX;
        int32_t wz = tz + chunkCenterZ;
        float approxY;
        int approxId;
        mapApproxHeight(&approxY, &approxId, gOverworld, &sn, wx >> 2, wz >> 2, 1, 1);
        cornerHeights[i] = (int32_t)approxY - 1;
    }
    /* sort descending (4 elements, plain insertion sort) */
    for (int i = 1; i < 4; i++) {
        int32_t key = cornerHeights[i];
        int j = i - 1;
        while (j >= 0 && cornerHeights[j] < key) {
            cornerHeights[j + 1] = cornerHeights[j];
            j--;
        }
        cornerHeights[j + 1] = key;
    }
    int32_t n = cornerHeights[2]; /* 3rd-highest = 2nd-lowest */
    if (n < 15) n = 15;

    int32_t airCount = 0;
    for (int i = 0; i < tpl->cellCount; i++) {
        const RPFrameCell *c = &tpl->cells[i];
        int32_t tx, ty, tz;
        mc_transformAround(c->x, c->y, c->z, mirror, rotation, pivotX, pivotZ, &tx, &ty, &tz);
        int32_t wx = tx + chunkCenterX;
        int32_t wy = ty + n;
        int32_t wz = tz + chunkCenterZ;

        if (c->isObsidian) {
            uint64_t blockRnd;
            mc_hashCodeSeed(&blockRnd, wx, wy, wz);
            if (nextFloat(&blockRnd) < 0.15F) {
                return 0; /* rolled Crying Obsidian - not completable */
            }
        } else {
            if (!airPocket) {
                return 0; /* natural terrain here is unknown - conservatively reject */
            }
            airCount++;
        }
    }

    if (outAirCount) *outAirCount = airCount;
    return 1;
}
