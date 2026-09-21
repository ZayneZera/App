package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.FilterConfig;
import com.zayne.seedfilter.scan.ScanStats;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanProgressScreen extends Screen {

    private final Screen parent;
    private final AtomicInteger attempts;
    private final AtomicBoolean cancelled;
    private final ScanStats stats;

    public ScanProgressScreen(Screen parent, AtomicInteger attempts, AtomicBoolean cancelled, ScanStats stats) {
        super(new LiteralText("Suche passende Seed..."));
        this.parent = parent;
        this.attempts = attempts;
        this.cancelled = cancelled;
        this.stats = stats;
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 70, 150, 20,
                new LiteralText("Abbrechen"), button -> {
            cancelled.set(true);
            this.client.openScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Versuche: " + attempts.get()), this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        renderFunnel(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    /**
     * Live "how many attempts survive each enabled criterion" funnel, so it's obvious which
     * criterion is the actual bottleneck instead of just watching a climbing counter.
     */
    private void renderFunnel(MatrixStack matrices) {
        if (stats == null) {
            return;
        }
        int total = Math.max(1, attempts.get());
        FilterConfig config = FilterConfig.get();
        int y = this.height / 2 + 10;
        if (config.villageEnabled) {
            y = drawFunnelLine(matrices, "Dorf", stats.passedVillage.get(), total, y);
        }
        if (config.ruinedPortalEnabled) {
            y = drawFunnelLine(matrices, "Ruined Portal", stats.passedRuinedPortal.get(), total, y);
        }
        if (config.buriedTreasureEnabled) {
            y = drawFunnelLine(matrices, "Buried Treasure", stats.passedTreasure.get(), total, y);
        }
        if (config.bastionEnabled) {
            y = drawFunnelLine(matrices, "Bastion", stats.passedBastion.get(), total, y);
        }
        if (config.fortressEnabled) {
            drawFunnelLine(matrices, "Fortress", stats.passedFortress.get(), total, y);
        }
    }

    private int drawFunnelLine(MatrixStack matrices, String label, int passed, int total, int y) {
        double pct = 100.0 * passed / total;
        drawCenteredText(matrices, this.textRenderer,
                new LiteralText(String.format("%s: %.2f%%", label, pct)), this.width / 2, y, 0x77AAFF);
        return y + 10;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
