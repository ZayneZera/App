package com.zayne.seedfilter;

/**
 * Rough per-seed match probability from the same structure-placement math validated against
 * cubiomes/real simulations this session (grid structures ≈ π·r²/spacing², buried treasure ≈
 * 1-(1-0.01)^chunksInCircle, bastion/fortress split the shared nether grid 60/40). Ignores biome
 * requirements entirely (e.g. buried treasure needing an actual beach), so it's an optimistic
 * estimate, not exact - used only to drive a progress bar, which keeps counting past 100% until
 * a seed is actually found rather than claiming to know exactly when one will show up.
 */
public class ProbabilityEstimator {

    private static final double VILLAGE_SPACING = 32;
    private static final double RUINED_PORTAL_SPACING = 40;
    private static final double NETHER_SPACING = 27; // bastion/fortress share this grid
    private static final double BASTION_WIN_FRACTION = 0.6;
    private static final double FORTRESS_WIN_FRACTION = 0.4;

    public static double expectedAttempts(SeedFilterConfig cfg) {
        double p = combinedProbability(cfg);
        return 1.0 / Math.max(p, 1e-12);
    }

    private static double combinedProbability(SeedFilterConfig cfg) {
        double p = 1.0;
        if (cfg.villageEnabled) {
            p *= areaProbability(cfg.villageMaxChunks, VILLAGE_SPACING);
        }
        if (cfg.ruinedPortalEnabled) {
            p *= areaProbability(cfg.ruinedPortalMaxChunks, RUINED_PORTAL_SPACING);
        }
        if (cfg.buriedTreasureEnabled) {
            p *= treasureProbability(cfg.buriedTreasureMaxChunks);
        }
        if (cfg.bastionEnabled) {
            int allowedTypes = (cfg.bastionAllowBridge ? 1 : 0) + (cfg.bastionAllowHousing ? 1 : 0)
                    + (cfg.bastionAllowStables ? 1 : 0) + (cfg.bastionAllowTreasure ? 1 : 0);
            p *= areaProbability(cfg.bastionMaxNetherChunks, NETHER_SPACING) * BASTION_WIN_FRACTION * (allowedTypes / 4.0);
        }
        if (cfg.fortressEnabled) {
            p *= areaProbability(cfg.fortressMaxNetherChunks, NETHER_SPACING) * FORTRESS_WIN_FRACTION;
        }
        return p;
    }

    private static double areaProbability(int maxChunks, double spacing) {
        return Math.min(1.0, Math.PI * maxChunks * maxChunks / (spacing * spacing));
    }

    private static double treasureProbability(int maxChunks) {
        int count = 0;
        for (int dz = -maxChunks; dz <= maxChunks; dz++) {
            for (int dx = -maxChunks; dx <= maxChunks; dx++) {
                if (dx * dx + dz * dz <= maxChunks * maxChunks) {
                    count++;
                }
            }
        }
        return 1 - Math.pow(0.99, count);
    }
}
