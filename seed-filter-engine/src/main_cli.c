#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "config.h"
#include "engine.h"

/* Headless entry point: called by the Minecraft mod via ProcessBuilder.
 * Usage: seedfilter --search [config_path]
 * While searching, prints "Progress: attempts=N reachedVillage=N passedVillage=N ..." every
 * ~300ms so the mod can show a live funnel instead of just a spinner. On success prints
 * "Seed: <seed>" / "SpawnX:" / "SpawnZ:" / "Cheats:" / "Attempts:"; "NoMatch" otherwise. */

static ScanStats g_stats;
static volatile int g_done = 0;

static void print_progress(void) {
    printf("Progress: attempts=%ld reachedVillage=%ld passedVillage=%ld "
           "reachedRuinedPortal=%ld passedRuinedPortal=%ld reachedTreasure=%ld passedTreasure=%ld "
           "reachedBastion=%ld passedBastion=%ld reachedFortress=%ld passedFortress=%ld\n",
           g_stats.attempts, g_stats.reachedVillage, g_stats.passedVillage,
           g_stats.reachedRuinedPortal, g_stats.passedRuinedPortal,
           g_stats.reachedTreasure, g_stats.passedTreasure,
           g_stats.reachedBastion, g_stats.passedBastion,
           g_stats.reachedFortress, g_stats.passedFortress);
    fflush(stdout);
}

static void *reporter_thread(void *unused) {
    (void) unused;
    while (!g_done) {
        print_progress();
        usleep(300000);
    }
    return NULL;
}

int main(int argc, char **argv) {
    const char *configPath = "seedfilter.cfg";
    if (argc >= 3) {
        configPath = argv[2];
    }

    if (argc < 2 || strcmp(argv[1], "--search") != 0) {
        fprintf(stderr, "Usage: %s --search [config_path]\n", argv[0]);
        return 1;
    }

    FilterConfig cfg;
    config_load(&cfg, configPath);

    memset(&g_stats, 0, sizeof(g_stats));
    volatile int cancel = 0;
    FilterResult result;

    pthread_t reporter;
    pthread_create(&reporter, NULL, reporter_thread, NULL);

    int found = engine_search(&cfg, &result, &g_stats, &cancel);

    g_done = 1;
    pthread_join(reporter, NULL);
    print_progress();

    if (found) {
        printf("Seed: %lld\n", (long long) result.seed);
        printf("SpawnX: %d\n", result.spawnX);
        printf("SpawnZ: %d\n", result.spawnZ);
        printf("Cheats: %d\n", result.enableCheats);
        printf("Creative: %d\n", result.creativeMode);
        if (result.rpFound) {
            printf("RpPortalX: %d\n", result.rpPortalX);
            printf("RpPortalZ: %d\n", result.rpPortalZ);
            printf("RpTemplateIndex: %d\n", result.rpTemplateIndex);
            printf("RpRotation: %d\n", result.rpRotation);
            printf("RpMirror: %d\n", result.rpMirror);
            printf("RpChestObsidian: %d\n", result.rpChestObsidian);
        }
        printf("MatchedCategories: %d\n", result.matchedCategories);
        printf("Attempts: %ld\n", g_stats.attempts);
        fflush(stdout);
        return 0;
    }

    printf("NoMatch\n");
    return 2;
}
