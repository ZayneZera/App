#include "frame.h"
#include "mc_random.h"
#include "biomes.h"

enum { RPTYPE_STANDARD, RPTYPE_DESERT, RPTYPE_JUNGLE, RPTYPE_SWAMP, RPTYPE_MOUNTAIN, RPTYPE_OCEAN };
enum { VP_LAND_SURFACE, VP_OCEAN_FLOOR, VP_PARTLY_BURIED, VP_IN_MOUNTAIN, VP_UNDERGROUND };

/* The 7 common templates (0-indexed into COMMON_PORTAL_STRUCTURE_IDS) we have real frame geometry
 * data for on the Java side (see RuinedPortalTemplates.java) - portal_5(4)/portal_7(6)/portal_10(9)
 * are intentionally absent (lying-flat or lava-damaged variants, see frame.h). */
static int isStandardTemplate(int commonIndex) {
    switch (commonIndex) {
        case 0: case 1: case 2: case 3: case 5: case 7: case 8:
            return 1;
        default:
            return 0;
    }
}

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

int rp_determinePlacement(uint64_t worldSeed, int biomeID, int32_t portalX, int32_t portalZ,
                           int *outTemplateIndex, int *outRotation, int *outMirror) {
    int32_t portalChunkX = portalX >> 4;
    int32_t portalChunkZ = portalZ >> 4;

    uint64_t rnd;
    mc_setCarverSeed(&rnd, worldSeed, portalChunkX, portalChunkZ);

    int portalType = classifyPortalType(biomeID);
    int verticalPlacement;
    switch (portalType) {
        case RPTYPE_DESERT:
            verticalPlacement = VP_PARTLY_BURIED;
            break;
        case RPTYPE_JUNGLE:
            nextFloat(&rnd); /* airPocket roll - not needed here, Java reads real blocks */
            verticalPlacement = VP_LAND_SURFACE;
            break;
        case RPTYPE_SWAMP:
            verticalPlacement = VP_OCEAN_FLOOR;
            break;
        case RPTYPE_MOUNTAIN: {
            int bl = nextFloat(&rnd) < 0.5F;
            verticalPlacement = bl ? VP_IN_MOUNTAIN : VP_LAND_SURFACE;
            if (!bl) nextFloat(&rnd);
            break;
        }
        case RPTYPE_OCEAN:
            verticalPlacement = VP_OCEAN_FLOOR;
            break;
        default: { /* STANDARD */
            int bl = nextFloat(&rnd) < 0.5F;
            verticalPlacement = bl ? VP_UNDERGROUND : VP_LAND_SURFACE;
            if (!bl) nextFloat(&rnd);
            break;
        }
    }

    /* Requested/accepted restriction: only accept portals actually exposed on land at the
     * surface, never underground/in-mountain/partly-buried/underwater - a portal the player can
     * walk straight up to. */
    if (verticalPlacement != VP_LAND_SURFACE) {
        return 0;
    }

    if (nextFloat(&rnd) < 0.05F) {
        return 0; /* rare (giant) template roll - not analyzed, reject */
    }
    int commonIndex = nextInt(&rnd, 10);
    if (!isStandardTemplate(commonIndex)) {
        return 0; /* portal_5/7/10 - no usable frame geometry, reject */
    }

    int rotation = nextInt(&rnd, 4);              /* 0=NONE,1=CW90,2=CW180,3=CCW90 */
    int mirror = (nextFloat(&rnd) < 0.5F) ? 0 : 2; /* 0=NONE, 2=FRONT_BACK */

    if (outTemplateIndex) *outTemplateIndex = commonIndex;
    if (outRotation) *outRotation = rotation;
    if (outMirror) *outMirror = mirror;
    return 1;
}
