package com.zayne.seedfilter;

import java.util.concurrent.atomic.AtomicLong;

/** Live funnel counters streamed from seedfilter.exe's "Progress:" lines while it searches. */
public class EngineStats {
    public final AtomicLong attempts = new AtomicLong();
    public final AtomicLong reachedVillage = new AtomicLong();
    public final AtomicLong passedVillage = new AtomicLong();
    public final AtomicLong reachedRuinedPortal = new AtomicLong();
    public final AtomicLong passedRuinedPortal = new AtomicLong();
    public final AtomicLong reachedTreasure = new AtomicLong();
    public final AtomicLong passedTreasure = new AtomicLong();
    public final AtomicLong reachedBastion = new AtomicLong();
    public final AtomicLong passedBastion = new AtomicLong();
    public final AtomicLong reachedFortress = new AtomicLong();
    public final AtomicLong passedFortress = new AtomicLong();

    /** Parses one "Progress: attempts=1234 reachedVillage=5 passedVillage=2 ..." line. */
    public void applyProgressLine(String line) {
        String body = line.substring("Progress:".length()).trim();
        for (String pair : body.split(" ")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = pair.substring(0, eq);
            long value;
            try {
                value = Long.parseLong(pair.substring(eq + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            switch (key) {
                case "attempts": attempts.set(value); break;
                case "reachedVillage": reachedVillage.set(value); break;
                case "passedVillage": passedVillage.set(value); break;
                case "reachedRuinedPortal": reachedRuinedPortal.set(value); break;
                case "passedRuinedPortal": passedRuinedPortal.set(value); break;
                case "reachedTreasure": reachedTreasure.set(value); break;
                case "passedTreasure": passedTreasure.set(value); break;
                case "reachedBastion": reachedBastion.set(value); break;
                case "passedBastion": passedBastion.set(value); break;
                case "reachedFortress": reachedFortress.set(value); break;
                case "passedFortress": passedFortress.set(value); break;
                default: break;
            }
        }
    }
}
