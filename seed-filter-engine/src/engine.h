#ifndef SEEDFILTER_ENGINE_H_
#define SEEDFILTER_ENGINE_H_

#include <stdint.h>
#include "config.h"

typedef struct {
    uint64_t seed;
    int spawnX;
    int spawnZ;
    int enableCheats;
} FilterResult;

/* Checks a single seed against cfg. Returns 1 and fills *out on match, 0 otherwise. */
int engine_check_seed(uint64_t seed, const FilterConfig *cfg, FilterResult *out);

/* Spawns cfg->threadCount worker threads scanning random seeds until one matches or
 * *cancelFlag becomes non-zero. attemptsCounter is incremented atomically per try (may be
 * NULL). Returns 1 and fills *out on success, 0 if cancelled. */
int engine_search(const FilterConfig *cfg, FilterResult *out, volatile long *attemptsCounter, volatile int *cancelFlag);

#endif
