package com.zayne.seedfilter.scan;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Funnel counters so we can see, live, WHICH enabled criterion is actually killing the match
 * rate (e.g. "Buried Treasure passes on only 2% of attempts because spawn usually isn't near
 * a beach") instead of just staring at a climbing attempt counter with no idea why.
 * Each counter only increments for seeds that already passed every earlier-checked criterion,
 * so it reads as a funnel, not an independent per-criterion rate.
 */
public class ScanStats {
    public final AtomicInteger attempts = new AtomicInteger();
    public final AtomicInteger passedVillage = new AtomicInteger();
    public final AtomicInteger passedRuinedPortal = new AtomicInteger();
    public final AtomicInteger passedTreasure = new AtomicInteger();
    public final AtomicInteger passedBastion = new AtomicInteger();
    public final AtomicInteger passedFortress = new AtomicInteger();
}
