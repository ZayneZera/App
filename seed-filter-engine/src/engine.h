#ifndef SEEDFILTER_ENGINE_H_
#define SEEDFILTER_ENGINE_H_

#include <stdint.h>
#include "config.h"

typedef struct {
    uint64_t seed;
    int spawnX;
    int spawnZ;
    int enableCheats;
    int creativeMode;

    /* Only meaningful when cfg->ruinedPortalEnabled && cfg->ruinedPortalFrameCheck: the found
     * portal's placement, for the mod to do a real (non-approximated) in-game completability
     * check once the player is teleported in - see frame.h. rpFound is 0 if frame check is off
     * or the portal isn't in a checkable placement/template. */
    int rpFound;
    int rpPortalX, rpPortalZ;
    int rpTemplateIndex, rpRotation, rpMirror;
    int rpChestObsidian; /* from loot_ruinedPortal() - reliable (no terrain dependency) unlike the
                            portal's placement Y, so the mod trusts this rather than trying to
                            open/read the real (lazily-generated) chest inventory. */

    /* Bitwise-OR of the CATEGORY_* flags (config.h) for every enabled top-level category this
     * seed actually passed. In AND mode (cfg->orMode == 0) this is always exactly the full set of
     * enabled categories (nothing else would have matched). In OR mode it can be any non-empty
     * subset - more than one bit set means the seed satisfies multiple independently, which is
     * what the mod's seed bank uses to tag a seed "OP". CATEGORY_LOOTING_RP is set independently
     * of all of that - see lootingFound below. */
    int matchedCategories;

    /* Independent side channel, checked on every seed regardless of cfg's ruined-portal settings
     * or whether the normal category match above succeeded: a Ruined Portal with a Looting II+
     * golden sword within a FIXED 8 chunks of spawn. Bastion/Fortress still gate this exactly like
     * the normal match (mandatory whenever enabled) - Village/BuriedTreasure do not gate it, they
     * are purely informational (CATEGORY_VILLAGE/CATEGORY_TREASURE end up in matchedCategories
     * alongside CATEGORY_LOOTING_RP when they also happen to match, for the seed bank's Looting
     * tab to show). When lootingFound, rpFound/rpPortalX/rpPortalZ/rpTemplateIndex/rpRotation/
     * rpMirror/rpChestObsidian above describe THIS looting portal (not the normal RP category's,
     * which this overrides if both happened to fire for the same seed - only one portal per result
     * is meaningful to actually join). */
    int lootingFound;
    int lootingLevel; /* 2 or 3, only meaningful when lootingFound */

    /* 1 if the seed satisfies the user's actual configured category filter (the pre-Looting-side-
     * channel behavior) - 0 if this result exists ONLY because of the independent Looting side
     * channel above (lootingFound is then always 1 too). The mod's single "join now" search uses
     * this to tell a real hit apart from an incidental Looting find that should be saved to the
     * seed bank and NOT joined - see SeedFilterMod's search flow. */
    int mainMatched;
} FilterResult;

/* Funnel counters, mirroring the mod's old ScanStats: "reached" = seeds that already passed
 * every earlier-checked criterion and are about to be evaluated against this one, "passed" =
 * the subset that also passed it. All fields are plain longs updated via __sync_fetch_and_add,
 * safe to read from another thread for live progress reporting (may be a tick stale, which is
 * fine for a progress display). */
typedef struct {
    volatile long attempts;
    volatile long reachedVillage, passedVillage;
    volatile long reachedRuinedPortal, passedRuinedPortal;
    volatile long reachedTreasure, passedTreasure;
    volatile long reachedBastion, passedBastion;
    volatile long reachedFortress, passedFortress;
} ScanStats;

/* Checks a single seed against cfg. Returns 1 and fills *out on match, 0 otherwise.
 * stats may be NULL (no funnel tracking). */
int engine_check_seed(uint64_t seed, const FilterConfig *cfg, FilterResult *out, ScanStats *stats);

/* Spawns cfg->threadCount worker threads scanning random seeds until one matches or
 * *cancelFlag becomes non-zero. stats may be NULL. Returns 1 and fills *out on success,
 * 0 if cancelled. */
int engine_search(const FilterConfig *cfg, FilterResult *out, ScanStats *stats, volatile int *cancelFlag);

#endif
