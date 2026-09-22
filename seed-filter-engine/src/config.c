#include "config.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#endif

/* The mod's in-game menu has no thread-count control and never writes thread_count= into the
 * .cfg file, so this default is what every real user actually runs at - a hardcoded 6 was
 * measured leaving most of a modern CPU idle (a Ryzen 7800X3D has 16 logical threads). Detect the
 * real core count instead, same as std::thread::hardware_concurrency() would. */
static int detect_cpu_threads(void) {
#ifdef _WIN32
    SYSTEM_INFO sysinfo;
    GetSystemInfo(&sysinfo);
    int n = (int) sysinfo.dwNumberOfProcessors;
#else
    long n = sysconf(_SC_NPROCESSORS_ONLN);
#endif
    return n > 0 ? (int) n : 6;
}

void config_set_defaults(FilterConfig *cfg) {
    cfg->villageEnabled = 1;
    cfg->villageMaxChunks = 8;

    cfg->ruinedPortalEnabled = 1;
    cfg->ruinedPortalMaxChunks = 5;
    cfg->ruinedPortalRequireLootingSword = 0;
    cfg->ruinedPortalFrameCheck = 0;

    cfg->buriedTreasureEnabled = 1;
    cfg->buriedTreasureMaxChunks = 4;
    cfg->buriedTreasureMinTnt = 0;
    cfg->buriedTreasureDiamondFilter = 0;
    cfg->buriedTreasureIronFilter = 0;
    cfg->buriedTreasureMinFish = 4;

    cfg->bastionEnabled = 1;
    cfg->bastionAllowBridge = 1;
    cfg->bastionAllowHousing = 1;
    cfg->bastionAllowStables = 1;
    cfg->bastionAllowTreasure = 1;
    cfg->bastionMaxNetherChunks = 6;

    cfg->fortressEnabled = 1;
    cfg->fortressMaxNetherChunks = 12;

    cfg->enableCheats = 0;
    cfg->creativeMode = 0;

    cfg->threadCount = detect_cpu_threads();

    cfg->orMode = 0;
}

static void trim(char *s) {
    size_t len = strlen(s);
    while (len > 0 && (s[len - 1] == '\n' || s[len - 1] == '\r' || s[len - 1] == ' ' || s[len - 1] == '\t')) {
        s[--len] = '\0';
    }
}

int config_load(FilterConfig *cfg, const char *path) {
    config_set_defaults(cfg);

    FILE *f = fopen(path, "r");
    if (!f) return 0;

    char line[256];
    while (fgets(line, sizeof(line), f)) {
        trim(line);
        char *eq = strchr(line, '=');
        if (!eq) continue;
        *eq = '\0';
        const char *key = line;
        int value = atoi(eq + 1);

        if (strcmp(key, "village_enabled") == 0) cfg->villageEnabled = value;
        else if (strcmp(key, "village_max_chunks") == 0) cfg->villageMaxChunks = value;
        else if (strcmp(key, "ruined_portal_enabled") == 0) cfg->ruinedPortalEnabled = value;
        else if (strcmp(key, "ruined_portal_max_chunks") == 0) cfg->ruinedPortalMaxChunks = value;
        else if (strcmp(key, "ruined_portal_looting_sword") == 0) cfg->ruinedPortalRequireLootingSword = value;
        else if (strcmp(key, "ruined_portal_frame_check") == 0) cfg->ruinedPortalFrameCheck = value;
        else if (strcmp(key, "buried_treasure_enabled") == 0) cfg->buriedTreasureEnabled = value;
        else if (strcmp(key, "buried_treasure_max_chunks") == 0) cfg->buriedTreasureMaxChunks = value;
        else if (strcmp(key, "buried_treasure_min_tnt") == 0) cfg->buriedTreasureMinTnt = value;
        else if (strcmp(key, "buried_treasure_diamond_filter") == 0) cfg->buriedTreasureDiamondFilter = value;
        else if (strcmp(key, "buried_treasure_iron_filter") == 0) cfg->buriedTreasureIronFilter = value;
        else if (strcmp(key, "buried_treasure_min_fish") == 0) cfg->buriedTreasureMinFish = value;
        else if (strcmp(key, "bastion_enabled") == 0) cfg->bastionEnabled = value;
        else if (strcmp(key, "bastion_allow_bridge") == 0) cfg->bastionAllowBridge = value;
        else if (strcmp(key, "bastion_allow_housing") == 0) cfg->bastionAllowHousing = value;
        else if (strcmp(key, "bastion_allow_stables") == 0) cfg->bastionAllowStables = value;
        else if (strcmp(key, "bastion_allow_treasure") == 0) cfg->bastionAllowTreasure = value;
        else if (strcmp(key, "bastion_max_nether_chunks") == 0) cfg->bastionMaxNetherChunks = value;
        else if (strcmp(key, "fortress_enabled") == 0) cfg->fortressEnabled = value;
        else if (strcmp(key, "fortress_max_nether_chunks") == 0) cfg->fortressMaxNetherChunks = value;
        else if (strcmp(key, "enable_cheats") == 0) cfg->enableCheats = value;
        else if (strcmp(key, "creative_mode") == 0) cfg->creativeMode = value;
        else if (strcmp(key, "thread_count") == 0) cfg->threadCount = value;
        else if (strcmp(key, "or_mode") == 0) cfg->orMode = value;
    }

    fclose(f);
    return 1;
}

int config_save(const FilterConfig *cfg, const char *path) {
    FILE *f = fopen(path, "w");
    if (!f) return 0;

    fprintf(f, "village_enabled=%d\n", cfg->villageEnabled);
    fprintf(f, "village_max_chunks=%d\n", cfg->villageMaxChunks);
    fprintf(f, "ruined_portal_enabled=%d\n", cfg->ruinedPortalEnabled);
    fprintf(f, "ruined_portal_max_chunks=%d\n", cfg->ruinedPortalMaxChunks);
    fprintf(f, "ruined_portal_looting_sword=%d\n", cfg->ruinedPortalRequireLootingSword);
    fprintf(f, "ruined_portal_frame_check=%d\n", cfg->ruinedPortalFrameCheck);
    fprintf(f, "buried_treasure_enabled=%d\n", cfg->buriedTreasureEnabled);
    fprintf(f, "buried_treasure_max_chunks=%d\n", cfg->buriedTreasureMaxChunks);
    fprintf(f, "buried_treasure_min_tnt=%d\n", cfg->buriedTreasureMinTnt);
    fprintf(f, "buried_treasure_diamond_filter=%d\n", cfg->buriedTreasureDiamondFilter);
    fprintf(f, "buried_treasure_iron_filter=%d\n", cfg->buriedTreasureIronFilter);
    fprintf(f, "buried_treasure_min_fish=%d\n", cfg->buriedTreasureMinFish);
    fprintf(f, "bastion_enabled=%d\n", cfg->bastionEnabled);
    fprintf(f, "bastion_allow_bridge=%d\n", cfg->bastionAllowBridge);
    fprintf(f, "bastion_allow_housing=%d\n", cfg->bastionAllowHousing);
    fprintf(f, "bastion_allow_stables=%d\n", cfg->bastionAllowStables);
    fprintf(f, "bastion_allow_treasure=%d\n", cfg->bastionAllowTreasure);
    fprintf(f, "bastion_max_nether_chunks=%d\n", cfg->bastionMaxNetherChunks);
    fprintf(f, "fortress_enabled=%d\n", cfg->fortressEnabled);
    fprintf(f, "fortress_max_nether_chunks=%d\n", cfg->fortressMaxNetherChunks);
    fprintf(f, "enable_cheats=%d\n", cfg->enableCheats);
    fprintf(f, "creative_mode=%d\n", cfg->creativeMode);
    fprintf(f, "thread_count=%d\n", cfg->threadCount);
    fprintf(f, "or_mode=%d\n", cfg->orMode);

    fclose(f);
    return 1;
}
