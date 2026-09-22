package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.EngineStats;
import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.ProbabilityEstimator;
import com.zayne.seedfilter.SeedFilterConfig;
import com.zayne.seedfilter.SeedFilterMod;
import com.zayne.seedfilter.TickPoller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shown when "Nächster Seed" is clicked in the pause menu. Starts the external engine
 * IMMEDIATELY (while still in the current world) and counts down 3 seconds. Once the countdown
 * reaches 0 it does NOT disconnect right away - it keeps waiting (still showing 0, still
 * searching) until the engine actually reports a match, and only then disconnects to the title
 * screen and auto-creates/joins the found seed.
 */
public class CountdownScreen extends Screen {

    private static final int COUNTDOWN_SECONDS = 3;

    private final long startTimeMillis;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<Process> processHolder = new AtomicReference<>();
    private final EngineStats stats = new EngineStats();
    private final SeedFilterConfig config = SeedFilterConfig.load(ExternalEngine.getConfigPath());
    private final CompletableFuture<ExternalEngine.Result> scanFuture;
    private boolean triggered = false;
    private double maxPctSeen = 0.0;
    private final SmoothedValue smoothedPct = new SmoothedValue();
    private final int startAttempt;

    public CountdownScreen() {
        this(1);
    }

    /** startAttempt lets a Ruined Portal auto-retry (see SeedFilterMod.retryForNewSeed) carry its
     * attempt count through this same disconnect/search/rejoin flow, so MAX_RP_RETRY_ATTEMPTS
     * still counts correctly across repeated "Not Approved" loops instead of resetting to 1 each
     * time the pause-menu "Nächster Seed" codepath is reused for it. */
    public CountdownScreen(int startAttempt) {
        super(new LiteralText("Nächster Seed"));
        this.startAttempt = startAttempt;
        this.startTimeMillis = System.currentTimeMillis();
        this.scanFuture = ExternalEngine.runAsync(processHolder, stats);
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 110, 150, 20,
                new LiteralText("Abbrechen"), button -> this.onClose()));
    }

    /**
     * Called for BOTH the Abbrechen button and an Escape keypress (shouldCloseOnEsc() is true
     * here) - without this override, Escape closed the screen without ever calling
     * ExternalEngine.cancel(), leaving seedfilter.exe (and its worker threads) running full-tilt
     * in the background indefinitely with no UI left pointing at it.
     */
    @Override
    public void onClose() {
        if (!triggered) {
            cancelled.set(true);
            ExternalEngine.cancel(processHolder);
        }
        this.client.openScreen(null);
    }

    @Override
    public void tick() {
        super.tick();
        if (cancelled.get() || triggered) {
            return;
        }
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        boolean countdownElapsed = elapsedMillis >= COUNTDOWN_SECONDS * 1000L;
        // Wait for BOTH the countdown AND an actual match - never disconnect just because the
        // timer ran out while the search is still going.
        if (countdownElapsed && scanFuture.isDone()) {
            triggered = true;
            MinecraftClient client = MinecraftClient.getInstance();
            // Two-step vanilla-style disconnect: tell the world/server first, then tear
            // down the client connection.
            client.execute(() -> {
                if (client.world != null) {
                    client.world.disconnect();
                }
                client.disconnect(new TitleScreen());
                waitThenCreate(client, 3);
            });
        }
    }

    private void waitThenCreate(MinecraftClient client, int settleTicksStart) {
        int[] settleTicks = {settleTicksStart};
        TickPoller.poll(() -> {
            if (client.world != null) {
                return false;
            }
            if (settleTicks[0] > 0) {
                settleTicks[0]--;
                return false;
            }
            scanFuture.thenAccept(result -> client.execute(() ->
                    SeedFilterMod.handleSearchResult(client, new TitleScreen(), result, startAttempt)));
            return true;
        });
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        int remaining = Math.max(0, COUNTDOWN_SECONDS - (int) (elapsedMillis / 1000));

        matrices.push();
        matrices.translate(this.width / 2.0, this.height / 2.0 - 40, 0);
        matrices.scale(4f, 4f, 4f);
        drawCenteredText(matrices, this.textRenderer, new LiteralText(String.valueOf(remaining)), 0, 0, 0xFFFFFF);
        matrices.pop();

        String status = remaining > 0 ? "Suche Seed im Hintergrund..." : "Warte auf Fund...";
        drawCenteredText(matrices, this.textRenderer, new LiteralText(status),
                this.width / 2, this.height / 2 + 10, 0xAAAAAA);
        renderFunnel(matrices);
        renderProgressBar(matrices, this.height / 2 + 70);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private void renderFunnel(MatrixStack matrices) {
        int y = this.height / 2 + 25;
        if (config.villageEnabled) {
            y = drawFunnelLine(matrices, "Dorf", stats.passedVillage.get(), stats.reachedVillage.get(), y);
        }
        if (config.ruinedPortalEnabled) {
            y = drawFunnelLine(matrices, "Ruined Portal", stats.passedRuinedPortal.get(), stats.reachedRuinedPortal.get(), y);
        }
        if (config.buriedTreasureEnabled) {
            y = drawFunnelLine(matrices, "Buried Treasure", stats.passedTreasure.get(), stats.reachedTreasure.get(), y);
        }
        if (config.bastionEnabled) {
            y = drawFunnelLine(matrices, "Bastion", stats.passedBastion.get(), stats.reachedBastion.get(), y);
        }
        if (config.fortressEnabled) {
            drawFunnelLine(matrices, "Fortress", stats.passedFortress.get(), stats.reachedFortress.get(), y);
        }
    }

    private int drawFunnelLine(MatrixStack matrices, String label, long passed, long reached, int y) {
        String pct = reached > 0 ? String.format(" (%.1f%%)", 100.0 * passed / reached) : "";
        drawCenteredText(matrices, this.textRenderer,
                new LiteralText(label + ": " + passed + "/" + reached + pct), this.width / 2, y, 0x77AAFF);
        return y + 10;
    }

    /**
     * Bar filled by attempts-so-far / statistically-expected-attempts, capped at 100% - the
     * estimate ignores things like biome requirements, so it can fill up well before an actual
     * match; once full it just stays full while the search keeps going.
     */
    private void renderProgressBar(MatrixStack matrices, int y) {
        int barW = 260, barH = 10;
        int x = this.width / 2 - barW / 2;
        double expectedAttempts = ProbabilityEstimator.expectedAttemptsForDisplay(config, stats);
        double rawPct = Math.min(100.0, 100.0 * stats.attempts.get() / expectedAttempts);
        maxPctSeen = Math.max(maxPctSeen, rawPct);
        double pct = smoothedPct.update(maxPctSeen);
        // Inner fillable area is only barW - 2 wide (1px border on each side), so the fill must
        // be capped to that, not to barW itself - at 100% "filled = barW" pushed 1px past the
        // right border.
        int filled = Math.min(barW - 2, (int) (barW * pct / 100.0));

        fill(matrices, x, y, x + barW, y + barH, DarkTheme.WIDGET_BORDER);
        fill(matrices, x + 1, y + 1, x + barW - 1, y + barH - 1, DarkTheme.WIDGET_BG);
        fill(matrices, x + 1, y + 1, x + 1 + filled, y + barH - 1, DarkTheme.ACCENT);

        drawCenteredText(matrices, this.textRenderer, new LiteralText(String.format("%.0f%%", pct)),
                this.width / 2, y + barH + 3, DarkTheme.TEXT_DIM);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
