#ifndef SEEDFILTER_FRAME_H_
#define SEEDFILTER_FRAME_H_

#include <stdint.h>
#include "generator.h"

/* Attempts to verify whether the Ruined Portal found at portalChunkX/Z has a completable frame:
 * all of its non-corner border positions must be Air or Obsidian (never Crying Obsidian).
 *
 * Returns 1 and fills *outAirCount (the number of those border positions that are Air - the
 * caller compares this against the chest's obsidian count) if the frame was checkable and free
 * of Crying Obsidian. Returns 0 - conservatively treated as "not completable" - when:
 *   - the portal rolled one of the 3 rare "giant" templates (not analyzed here),
 *   - it rolled one of the 3 common templates we don't have clean frame geometry for (portal_5
 *     and portal_10 are "lying flat" variants with different geometry entirely; portal_7 is a
 *     lava-damaged variant with no clean obsidian/air border), or
 *   - Crying Obsidian was actually rolled at one of the border positions, or
 *   - a border position that the template marks "air" landed on a placement where vanilla's
 *     BlockIgnoreStructureProcessor would leave the *natural* terrain there instead of forcing
 *     air (properties.airPocket == false) - we have no real terrain block data to check what's
 *     actually there, so we don't guess.
 *
 * Two approximations, accepted after discussion: the world seed's terrain surface height is
 * sourced from cubiomes' mapApproxHeight() rather than vanilla's exact block-level heightmap scan
 * (chunkGenerator.getHeight() + getColumnSample()), and the height/terrain-sample point is taken
 * as the portal's chunk-center rather than its exact (rotation/mirror-dependent) bounding-box
 * center. Both can shift the portal's placement Y by a small amount, which in turn changes every
 * per-block Crying-Obsidian roll (those are seeded from the exact world position) - so results
 * here are a best-effort approximation, not bit-exact like the loot table filters. */
int rp_checkFrame(const Generator *gOverworld, uint64_t worldSeed, int biomeID, int32_t portalChunkX, int32_t portalChunkZ, int32_t *outAirCount);

#endif
