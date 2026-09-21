package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.EngineStats;
import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.ProbabilityEstimator;
import com.zayne.seedfilter.SeedFilterConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.concurrent.atomic.AtomicReference;

public class ScanProgressScreen extends Screen {

    private final Screen parent;
    private final AtomicReference<Process> processHolder;
    private final EngineStats stats;
    private final SeedFilterConfig config;
    private double maxPctSeen = 0.0;
    private final SmoothedValue smoothedPct = new SmoothedValue();
    private final EmaSmoother smoothedExpectedAttempts = new EmaSmoother(2.5);

    public ScanProgressScreen(Screen parent, AtomicReference<Process> processHolder, EngineStats stats) {
        super(new LiteralText("Suche passende Seed..."));
        this.parent = parent;
        this.processHolder = processHolder;
        this.stats = stats;
        this.config = SeedFilterConfig.load(ExternalEngine.getConfigPath());
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 110, 150, 20,
                new LiteralText("Abbrechen"), button -> {
            ExternalEngine.cancel(processHolder);
            this.client.openScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Versuche: " + stats.attempts.get()),
                this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        renderFunnel(matrices);
        renderProgressBar(matrices, this.height / 2 + 70);
        super.render(matrices, mouseX, mouseY, delta);
    }

    /**
     * Bar filled by attempts-so-far / statistically-expected-attempts (from ProbabilityEstimator),
     * capped at 100% - the estimate ignores things like biome requirements, so it can fill up
     * well before an actual match; once full it just stays full while the search keeps going.
     *
     * expectedAttempts is recomputed live and can jump UP once a criterion crosses the sample
     * threshold and switches from the static guess to its real (often much rarer) empirical pass
     * rate - that alone would make an already-100% bar drop back down mid-search. maxPctSeen
     * latches the highest value shown so far so the bar only ever climbs, never regresses.
     */
    private void renderProgressBar(MatrixStack matrices, int y) {
        int barW = 300, barH = 10;
        int x = this.width / 2 - barW / 2;
        double liveExpectedAttempts = ProbabilityEstimator.expectedAttemptsForDisplay(config, stats);
        double expectedAttempts = smoothedExpectedAttempts.update(liveExpectedAttempts);
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

    /** Live "passed/reached" funnel per enabled criterion, read from the exe's Progress: lines. */
    private void renderFunnel(MatrixStack matrices) {
        int y = this.height / 2 + 10;
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

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
