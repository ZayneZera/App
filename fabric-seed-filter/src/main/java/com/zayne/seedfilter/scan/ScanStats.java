package com.zayne.seedfilter.scan;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Funnel counters so we can see, live, WHICH enabled criterion is actually killing the match
 * rate (e.g. "Buried Treasure passes on only 2% of attempts because spawn usually isn't near
 * a beach") instead of just staring at a climbing attempt counter with no idea why.
 * "reached" = seeds that already passed every earlier-checked criterion and are about to be
 * evaluated against this one; "passed" = the subset of those that also passed this one. Both
 * are tracked (not just a percentage of total attempts) because later criteria can have a
 * tiny sample size - e.g. if only 60 of 5000 attempts ever reach the Bastion check, "0.00%"
 * looks alarming but "0/60" makes it obvious it's just a small sample, not a bug.
 */
public class ScanStats {
    public final AtomicInteger attempts = new AtomicInteger();

    public final AtomicInteger reachedVillage = new AtomicInteger();
    public final AtomicInteger passedVillage = new AtomicInteger();
    public final AtomicInteger reachedRuinedPortal = new AtomicInteger();
    public final AtomicInteger passedRuinedPortal = new AtomicInteger();
    public final AtomicInteger reachedTreasure = new AtomicInteger();
    public final AtomicInteger passedTreasure = new AtomicInteger();
    public final AtomicInteger reachedBastion = new AtomicInteger();
    public final AtomicInteger passedBastion = new AtomicInteger();
    public final AtomicInteger reachedFortress = new AtomicInteger();
    public final AtomicInteger passedFortress = new AtomicInteger();
}
