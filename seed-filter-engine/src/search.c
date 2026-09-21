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
    unsigned int seedSeed;
} WorkerArgs;

static void *worker(void *argPtr) {
    WorkerArgs *args = (WorkerArgs *) argPtr;
    unsigned int rngState = args->seedSeed;

    while (!*args->cancelFlag && !atomic_load(args->foundFlag)) {
        /* xorshift64* for a fast, thread-local 64-bit seed stream (uniqueness across threads
         * only needs distinct starting state, not cryptographic quality). */
        static _Thread_local uint64_t xstate = 0;
        if (xstate == 0) {
            xstate = ((uint64_t) rngState << 32) ^ (uint64_t) time(NULL) ^ (uint64_t) (uintptr_t) &xstate;
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

    for (int i = 0; i < threadCount; i++) {
        args[i].cfg = cfg;
        args[i].out = out;
        args[i].stats = stats;
        args[i].cancelFlag = cancelFlag;
        args[i].foundFlag = &foundFlag;
        args[i].seedSeed = (unsigned int) (i * 2654435761u + 1);
        pthread_create(&threads[i], NULL, worker, &args[i]);
    }

    for (int i = 0; i < threadCount; i++) {
        pthread_join(threads[i], NULL);
    }

    free(threads);
    free(args);

    return atomic_load(&foundFlag);
}
