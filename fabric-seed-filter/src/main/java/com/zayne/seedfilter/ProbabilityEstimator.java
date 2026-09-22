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
 * Fix: as live samples come in (EngineStats' reached/passed counts, streamed from the exe's real
 * cubiomes-backed checks), blend towards the criterion's OWN measured pass rate instead of the
 * static guess - that's ground truth, not an approximation, and automatically accounts for biome
 * effects the static formula can't model. See rate() for why this is a gradual Bayesian blend
 * rather than a hard switch once enough samples exist.
 */
public class ProbabilityEstimator {

    private static final double VILLAGE_SPACING = 32;
    private static final double RUINED_PORTAL_SPACING = 40;
    private static final double NETHER_SPACING = 27; // bastion/fortress share this grid
    private static final double BASTION_WIN_FRACTION = 0.6;
    private static final double FORTRESS_WIN_FRACTION = 0.4;

    /**
     * How many "virtual" trials the static guess is worth when blending with the live empirical
     * rate (see rate() below) - not a hard sample-count threshold.
     */
    private static final double PRIOR_WEIGHT = 25;

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
     * Bayesian blend: treats the static guess as if it were already backed by PRIOR_WEIGHT
     * virtual trials, then folds in the real observed (passed, reached) on top of that.
     * Equivalent to a Beta-prior update, and it never collapses to a hard 0 - which matters a lot
     * for a genuinely rare criterion like Fortress. A previous version switched fully over to the
     * raw empirical rate (passed/reached) once reached crossed a threshold - but a rare structure
     * routinely sits at e.g. 0/47 for a long stretch simply because it hasn't gotten lucky yet,
     * NOT because its true rate is 0. Trusting that raw 0 collapsed the combined probability to
     * 0, sent expectedAttempts to its ceiling, and froze the whole bar's growth well before an
     * actual match - even though the other (non-zero) criteria were still progressing fine. Here,
     * a 0/47 criterion still contributes a small but nonzero rate that only fades toward true 0
     * gradually as more zero-hit samples pile up, so the bar keeps crawling forward instead of
     * stalling on one unlucky criterion.
     */
    private static double rate(long reached, long passed, double staticFallback) {
        return (passed + staticFallback * PRIOR_WEIGHT) / (reached + PRIOR_WEIGHT);
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
