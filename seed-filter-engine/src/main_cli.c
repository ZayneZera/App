#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "config.h"
#include "engine.h"

/* Headless entry point: called by the Minecraft mod via ProcessBuilder.
 * Usage: seedfilter --search [config_path]
 * Prints progress lines "Progress: N" periodically (not yet - single-shot for now),
 * then on success: "Seed: <seed>" and "Cheats: 0|1", or "NoMatch" if cancelled/failed. */
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

    volatile long attempts = 0;
    volatile int cancel = 0;
    FilterResult result;

    if (engine_search(&cfg, &result, &attempts, &cancel)) {
        printf("Seed: %lld\n", (long long) result.seed);
        printf("SpawnX: %d\n", result.spawnX);
        printf("SpawnZ: %d\n", result.spawnZ);
        printf("Cheats: %d\n", result.enableCheats);
        printf("Attempts: %ld\n", attempts);
        fflush(stdout);
        return 0;
    }

    printf("NoMatch\n");
    return 2;
}
