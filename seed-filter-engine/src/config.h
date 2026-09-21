#ifndef SEEDFILTER_CONFIG_H_
#define SEEDFILTER_CONFIG_H_

typedef struct {
    int villageEnabled;
    int villageMaxChunks;

    int ruinedPortalEnabled;
    int ruinedPortalMaxChunks;
    int ruinedPortalRequireLootingSword;

    int buriedTreasureEnabled;
    int buriedTreasureMaxChunks;

    int bastionEnabled;
    int bastionAllowBridge;
    int bastionAllowHousing;
    int bastionAllowStables;
    int bastionAllowTreasure;
    int bastionMaxNetherChunks;

    int fortressEnabled;
    int fortressMaxNetherChunks;

    int enableCheats;

    int threadCount;
} FilterConfig;

void config_set_defaults(FilterConfig *cfg);
/* Returns 1 on success (file existed and was parsed), 0 if it used defaults because the file was missing. */
int config_load(FilterConfig *cfg, const char *path);
int config_save(const FilterConfig *cfg, const char *path);

#endif
