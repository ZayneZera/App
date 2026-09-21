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
