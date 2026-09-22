#ifndef SEEDFILTER_FRAME_H_
#define SEEDFILTER_FRAME_H_

#include <stdint.h>

/* Determines the Ruined Portal's placement (template/rotation/mirror) for the portal found at
 * portalX/portalZ (the raw block position from cubiomes' getStructurePos - NOT re-derived to a
 * chunk center, see the comment in rp_determinePlacement()'s definition for why that distinction
 * matters), WITHOUT trying to predict its actual block contents.
 *
 * Earlier versions of this feature tried to predict Crying Obsidian and frame completability
 * purely from the world seed, using cubiomes' mapApproxHeight() to estimate the portal's exact
 * placement Y. That turned out to be unreliable in practice - even a 1-2 block height error means
 * hashing a completely different (unrelated) world position for every per-block Crying Obsidian
 * roll, not just a "slightly off" one. Real-seed testing confirmed height errors of several
 * blocks happen often enough (caves/ravines near the portal site throw the approximation off
 * further than expected) to make that approach unreliable as a pass/fail verdict.
 *
 * The actual fix (validated against Seed Quarry, an existing community seed-finder, which does
 * the same thing for exact portal height/frame checks): once the mod has teleported the player
 * into a real generated world for a candidate seed, the JAVA side can read the true world height
 * and the true block states directly - no approximation needed. This function now only produces
 * the RNG-determined (and therefore exact, no terrain dependency) template/rotation/mirror choice,
 * which the mod combines with a real in-game heightmap query and real block reads to do the
 * actual Crying-Obsidian/completability verdict.
 *
 * Returns 1 and fills *outTemplateIndex (0-9, matching RuinedPortalFeature's
 * COMMON_PORTAL_STRUCTURE_IDS order), *outRotation (0=NONE,1=CW90,2=CW180,3=CCW90) and *outMirror
 * (0=NONE, 2=FRONT_BACK) if this portal is checkable: it rolled ON_LAND_SURFACE placement (not
 * underground/in-mountain/partly-buried/underwater - matches the "must be airborne" requirement)
 * and one of the 7 common templates (portal_1/2/3/4/6/8/9) we have real frame geometry data for
 * (portal_5/10 are "lying flat" variants with different geometry, portal_7 is lava-damaged with no
 * clean frame, and the 3 rare "giant" templates aren't analyzed).
 *
 * Returns 0 - conservatively "not checkable" - otherwise. */
int rp_determinePlacement(uint64_t worldSeed, int biomeID, int32_t portalX, int32_t portalZ,
                           int *outTemplateIndex, int *outRotation, int *outMirror);

#endif
