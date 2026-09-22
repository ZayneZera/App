#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include "config.h"
#include "engine.h"

/* Headless entry point: called by the Minecraft mod via ProcessBuilder.
 *
 * Usage: seedfilter --search [config_path]
 * While searching, prints "Progress: attempts=N reachedVillage=N passedVillage=N ..." every
 * ~300ms so the mod can show a live funnel instead of just a spinner. On success prints
 * "Seed: <seed>" / "SpawnX:" / "SpawnZ:" / "Cheats:" / "Attempts:"; "NoMatch" otherwise. Exits
 * after the first match (or on cancellation, which the mod does by killing the process - there's
 * no in-process cancel signal here, same as --search-bank below).
 *
 * Usage: seedfilter --search-bank [config_path]
 * For the mod's seed bank "find and save, don't join" mode: like --search, but never stops after
 * the first match - keeps searching and printing one full result block per match indefinitely
 * (Progress: stats stay cumulative across all of them), until the mod kills the process. Each
 * result block ends with a "MatchEnd: 1" line so the mod's stdout reader knows one match's fields
 * are complete and the next Seed:/SpawnX:/... it sees starts a new one, rather than needing to
 * infer that from seeing "Seed:" again. */

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

static void print_result(const FilterResult *result, int withMatchEnd) {
    printf("Seed: %lld\n", (long long) result->seed);
    printf("SpawnX: %d\n", result->spawnX);
    printf("SpawnZ: %d\n", result->spawnZ);
    printf("Cheats: %d\n", result->enableCheats);
    printf("Creative: %d\n", result->creativeMode);
    if (result->rpFound) {
        printf("RpPortalX: %d\n", result->rpPortalX);
        printf("RpPortalZ: %d\n", result->rpPortalZ);
        printf("RpTemplateIndex: %d\n", result->rpTemplateIndex);
        printf("RpRotation: %d\n", result->rpRotation);
        printf("RpMirror: %d\n", result->rpMirror);
        printf("RpChestObsidian: %d\n", result->rpChestObsidian);
    }
    printf("MatchedCategories: %d\n", result->matchedCategories);
    printf("MainMatched: %d\n", result->mainMatched);
    if (result->lootingFound) {
        printf("LootingLevel: %d\n", result->lootingLevel);
    }
    printf("Attempts: %ld\n", g_stats.attempts);
    if (withMatchEnd) {
        printf("MatchEnd: 1\n");
    }
    fflush(stdout);
}

int main(int argc, char **argv) {
    const char *configPath = "seedfilter.cfg";
    if (argc >= 3) {
        configPath = argv[2];
    }

    int bankMode;
    if (argc >= 2 && strcmp(argv[1], "--search") == 0) {
        bankMode = 0;
    } else if (argc >= 2 && strcmp(argv[1], "--search-bank") == 0) {
        bankMode = 1;
    } else {
        fprintf(stderr, "Usage: %s --search|--search-bank [config_path]\n", argv[0]);
        return 1;
    }

    FilterConfig cfg;
    config_load(&cfg, configPath);

    memset(&g_stats, 0, sizeof(g_stats));
    volatile int cancel = 0; /* never set from in here - the mod cancels by killing the process */

    pthread_t reporter;
    pthread_create(&reporter, NULL, reporter_thread, NULL);

    if (bankMode) {
        while (1) {
            FilterResult result;
            if (!engine_search(&cfg, &result, &g_stats, &cancel)) {
                break; /* only happens if *cancel were set, which nothing here ever does */
            }
            print_result(&result, 1);
        }
        g_done = 1;
        pthread_join(reporter, NULL);
        return 0;
    }

    FilterResult result;
    int found = engine_search(&cfg, &result, &g_stats, &cancel);

    g_done = 1;
    pthread_join(reporter, NULL);
    print_progress();

    if (found) {
        print_result(&result, 0);
        return 0;
    }

    printf("NoMatch\n");
    return 2;
}
