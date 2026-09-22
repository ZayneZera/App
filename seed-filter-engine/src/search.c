#include "engine.h"

#include <pthread.h>
#include <stdatomic.h>
#include <stdlib.h>
#include <time.h>

typedef struct {
    const FilterConfig *cfg;
    FilterResult *out;
    ScanStats *stats;
    volatile int *cancelFlag;
    atomic_int *foundFlag;
    uint64_t seedSeed;
} WorkerArgs;

/* Bumped once per engine_search() call (see below) and mixed into each worker's starting state.
 * Needed because the seed bank's repeat-search mode calls engine_search() back-to-back in a tight
 * loop, spawning brand new threads each time - a fresh thread's stack often gets allocated at the
 * exact same address as the last one's did, and time(NULL) only has 1-second resolution, so
 * without this a worker's "random" starting xorshift state (and therefore its entire seed
 * sequence) could come out byte-for-byte identical run to run, which is exactly what happened in
 * testing: the same couple of seeds kept coming back instead of new ones. */
static atomic_ulong g_searchGeneration = 0;

static void *worker(void *argPtr) {
    WorkerArgs *args = (WorkerArgs *) argPtr;
    uint64_t rngState = args->seedSeed;

    while (!*args->cancelFlag && !atomic_load(args->foundFlag)) {
        /* xorshift64* for a fast, thread-local 64-bit seed stream (uniqueness across threads
         * only needs distinct starting state, not cryptographic quality). */
        static _Thread_local uint64_t xstate = 0;
        if (xstate == 0) {
            struct timespec ts;
            clock_gettime(CLOCK_MONOTONIC, &ts);
            xstate = rngState ^ ((uint64_t) ts.tv_sec << 32) ^ (uint64_t) ts.tv_nsec
                     ^ (uint64_t) (uintptr_t) &xstate;
            if (xstate == 0) xstate = 0x9E3779B97F4A7C15ULL; /* xorshift can't start at 0 */
        }
        xstate ^= xstate << 13;
        xstate ^= xstate >> 7;
        xstate ^= xstate << 17;
        uint64_t seed = xstate;

        if (args->stats) {
            __sync_fetch_and_add(&args->stats->attempts, 1);
        }

        FilterResult result;
        if (engine_check_seed(seed, args->cfg, &result, args->stats)) {
            int expected = 0;
            if (atomic_compare_exchange_strong(args->foundFlag, &expected, 1)) {
                *args->out = result;
            }
            return NULL;
        }
    }
    return NULL;
}

int engine_search(const FilterConfig *cfg, FilterResult *out, ScanStats *stats, volatile int *cancelFlag) {
    int threadCount = cfg->threadCount > 0 ? cfg->threadCount : 6;
    pthread_t *threads = malloc(sizeof(pthread_t) * threadCount);
    WorkerArgs *args = malloc(sizeof(WorkerArgs) * threadCount);
    atomic_int foundFlag = 0;
    uint64_t generation = atomic_fetch_add(&g_searchGeneration, 1);

    for (int i = 0; i < threadCount; i++) {
        args[i].cfg = cfg;
        args[i].out = out;
        args[i].stats = stats;
        args[i].cancelFlag = cancelFlag;
        args[i].foundFlag = &foundFlag;
        args[i].seedSeed = ((uint64_t) i * 2654435761ULL + 1) ^ (generation * 0x2545F4914F6CDD1DULL);
        pthread_create(&threads[i], NULL, worker, &args[i]);
    }

    for (int i = 0; i < threadCount; i++) {
        pthread_join(threads[i], NULL);
    }

    free(threads);
    free(args);

    return atomic_load(&foundFlag);
}
