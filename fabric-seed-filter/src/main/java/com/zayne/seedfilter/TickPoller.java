package com.zayne.seedfilter;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Runs a check once per REAL client tick until it reports done, instead of the self-recursive
 * "poll again via client.execute(() -> pollAgain())" pattern used all over this mod before -
 * which turned out to be a real bug: MinecraftClient.execute() runs its task immediately/
 * synchronously whenever it's already called from the client thread (only genuinely defers to
 * the next tick when called from a different thread). Every "wait until ready" loop here started
 * from a background thread (a real deferral, so it worked), but joining a seed straight from the
 * seed bank's mouseClicked handler calls it directly from the client thread - the recursion never
 * yields back to the game loop at all, the condition it's waiting on (client.player becoming
 * non-null) never gets a chance to become true, and it stack-overflows instead of waiting.
 * Registering into the real END_CLIENT_TICK event instead guarantees an actual tick boundary
 * between checks, no matter which thread first calls poll().
 */
public final class TickPoller {

    private static final List<BooleanSupplier> pending = new ArrayList<>();
    private static boolean registered = false;

    /** Runs `check` once per client tick (starting next tick, never this call) until it returns
     * true. Safe to call from any thread. */
    public static synchronized void poll(BooleanSupplier check) {
        ensureRegistered();
        synchronized (pending) {
            pending.add(check);
        }
    }

    private static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            List<BooleanSupplier> due;
            synchronized (pending) {
                if (pending.isEmpty()) {
                    return;
                }
                due = new ArrayList<>(pending);
                pending.clear();
            }
            for (BooleanSupplier check : due) {
                if (!check.getAsBoolean()) {
                    synchronized (pending) {
                        pending.add(check);
                    }
                }
            }
        });
    }

    private TickPoller() {
    }
}
