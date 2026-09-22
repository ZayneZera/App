#ifndef SEEDFILTER_CONFIG_H_
#define SEEDFILTER_CONFIG_H_

typedef struct {
    int villageEnabled;
    int villageMaxChunks;

    int ruinedPortalEnabled;
    int ruinedPortalMaxChunks;
    int ruinedPortalRequireLootingSword;
    int ruinedPortalFrameCheck;          /* approximate - see frame.h for caveats */

    int buriedTreasureEnabled;
    int buriedTreasureMaxChunks;
    int buriedTreasureMinTnt;           /* 0-2, 0 = no TNT requirement */
    int buriedTreasureDiamondFilter;    /* requires >= 3 diamonds */
    int buriedTreasureIronFilter;       /* requires >= 10 iron, or >= 7 if diamond filter is also on */
    int buriedTreasureMinFish;          /* 4-8; every chest has 4-8 fish already, so 4 = no-op */

    int bastionEnabled;
    int bastionAllowBridge;
    int bastionAllowHousing;
    int bastionAllowStables;
    int bastionAllowTreasure;
    int bastionMaxNetherChunks;

    int fortressEnabled;
    int fortressMaxNetherChunks;

    int enableCheats;
    int creativeMode;

    int threadCount;

    /* 0 (default): a match requires every ENABLED top-level category (village/ruinedPortal/
     * buriedTreasure/bastion+fortress) to pass, exactly like before. 1: a match requires only
     * ANY ONE of the enabled categories to pass - engine_check_seed then evaluates every enabled
     * category (no early-exit-on-first-failure) so FilterResult.matchedCategories can report
     * exactly which ones a given seed actually hit, e.g. for the seed bank's "OP" tag when a seed
     * satisfies more than were strictly required. */
    int orMode;
} FilterConfig;

/* Bit flags for FilterResult.matchedCategories - which top-level category(ies) a seed passed. */
#define CATEGORY_VILLAGE        (1 << 0)
#define CATEGORY_RUINED_PORTAL  (1 << 1)
#define CATEGORY_TREASURE       (1 << 2)
#define CATEGORY_BASTION        (1 << 3)
#define CATEGORY_FORTRESS       (1 << 4)

void config_set_defaults(FilterConfig *cfg);
/* Returns 1 on success (file existed and was parsed), 0 if it used defaults because the file was missing. */
int config_load(FilterConfig *cfg, const char *path);
int config_save(const FilterConfig *cfg, const char *path);

#endif
