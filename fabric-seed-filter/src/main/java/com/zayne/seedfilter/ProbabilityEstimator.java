package com.zayne.seedfilter;

/**
 * Per-seed match probability, combined by multiplying each enabled criterion's chance
 * (assuming independence, same as the math validated against cubiomes this session).
 *
 * The static area/coinflip formulas (grid structures ~ π·r²/spacing², buried treasure ~
 * 1-(1-0.01)^chunksInCircle, bastion/fortress split the shared nether grid 60/40) ignore biome
 * requirements entirely - buried treasure needs an actual beach chunk, which the formula has no
 * way to know about, so on its own it was wildly optimistic (~39% predicted at a 4-chunk radius
 * vs. ~1.2% actually observed this session - the progress bar hit 100% almost immediately).
 *
 * Fix: once a criterion has enough live samples (EngineStats' reached/passed counts, streamed
 * from the exe's real cubiomes-backed checks), use ITS OWN measured pass rate instead of the
 * static guess - that's ground truth, not an approximation, and automatically accounts for
 * biome effects the static formula can't model. Only criteria still short on samples fall back
 * to the static estimate.
 */
public class ProbabilityEstimator {

    private static final double VILLAGE_SPACING = 32;
    private static final double RUINED_PORTAL_SPACING = 40;
    private static final double NETHER_SPACING = 27; // bastion/fortress share this grid
    private static final double BASTION_WIN_FRACTION = 0.6;
    private static final double FORTRESS_WIN_FRACTION = 0.4;

    /** Below this many "reached" samples, a criterion's empirical rate is too noisy to trust. */
    private static final long MIN_SAMPLES = 25;

    /**
     * The estimate is a 50% expectation (on average, half of matching seeds get found before
     * this many attempts and half after) - displaying it directly means the progress bar hits
     * 100% at "average" luck and reads as stuck/broken on the unlucky-but-still-normal runs that
     * take longer. Padding the denominator by 50% makes the bar hit 100% only once 150% of the
     * expected attempts have run, so an average-length search shows ~67% instead of 100%.
     */
    private static final double DISPLAY_SAFETY_MARGIN = 1.5;

    public static double expectedAttempts(SeedFilterConfig cfg, EngineStats stats) {
        double p = combinedProbability(cfg, stats);
        return 1.0 / Math.max(p, 1e-12);
    }

    /** Same as {@link #expectedAttempts}, but padded for progress-bar display - see DISPLAY_SAFETY_MARGIN. */
    public static double expectedAttemptsForDisplay(SeedFilterConfig cfg, EngineStats stats) {
        return expectedAttempts(cfg, stats) * DISPLAY_SAFETY_MARGIN;
    }

    private static double combinedProbability(SeedFilterConfig cfg, EngineStats stats) {
        double p = 1.0;
        if (cfg.villageEnabled) {
            p *= rate(stats.reachedVillage.get(), stats.passedVillage.get(),
                    areaProbability(cfg.villageMaxChunks, VILLAGE_SPACING));
        }
        if (cfg.ruinedPortalEnabled) {
            p *= rate(stats.reachedRuinedPortal.get(), stats.passedRuinedPortal.get(),
                    areaProbability(cfg.ruinedPortalMaxChunks, RUINED_PORTAL_SPACING));
        }
        if (cfg.buriedTreasureEnabled) {
            p *= rate(stats.reachedTreasure.get(), stats.passedTreasure.get(),
                    treasureProbability(cfg.buriedTreasureMaxChunks));
        }
        if (cfg.bastionEnabled) {
            int allowedTypes = (cfg.bastionAllowBridge ? 1 : 0) + (cfg.bastionAllowHousing ? 1 : 0)
                    + (cfg.bastionAllowStables ? 1 : 0) + (cfg.bastionAllowTreasure ? 1 : 0);
            double staticGuess = areaProbability(cfg.bastionMaxNetherChunks, NETHER_SPACING) * BASTION_WIN_FRACTION * (allowedTypes / 4.0);
            p *= rate(stats.reachedBastion.get(), stats.passedBastion.get(), staticGuess);
        }
        if (cfg.fortressEnabled) {
            double staticGuess = areaProbability(cfg.fortressMaxNetherChunks, NETHER_SPACING) * FORTRESS_WIN_FRACTION;
            p *= rate(stats.reachedFortress.get(), stats.passedFortress.get(), staticGuess);
        }
        return p;
    }

    /**
     * Blends linearly from the static guess to the empirical rate as samples accumulate, instead
     * of switching over in one step at reached == MIN_SAMPLES. A hard switch meant every enabled
     * criterion's own transition (each lands at a different attempt count) yanked the combined
     * probability - and therefore expectedAttempts - by a large amount in a single frame, which
     * showed up as the progress bar's growth suddenly stalling right at that point.
     */
    private static double rate(long reached, long passed, double staticFallback) {
        if (reached == 0) {
            return staticFallback;
        }
        double empirical = (double) passed / reached;
        double weight = Math.min(1.0, reached / (double) MIN_SAMPLES);
        return staticFallback * (1 - weight) + empirical * weight;
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
