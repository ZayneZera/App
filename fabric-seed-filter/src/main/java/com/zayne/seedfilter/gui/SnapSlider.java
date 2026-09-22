package com.zayne.seedfilter.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.function.IntConsumer;

/**
 * Integer slider: snaps to whole steps and draws its own tick marks + accent-colored handle
 * instead of the vanilla button texture. Click-anywhere-to-jump and drag both go through
 * mouseClicked/mouseDragged (the stable public Element methods) rather than SliderWidget's own
 * internal value-from-mouse hook, which isn't overridable in this Minecraft version.
 *
 * Handle range spans the FULL track width (no inset padding) so at min/max the handle sits
 * flush against the track's edges instead of stopping short with a leftover gap.
 */
public class SnapSlider extends SliderWidget {
    private static final int HANDLE_WIDTH = 5;

    private final int min;
    private final int max;
    private final String label;
    private final IntConsumer onChange;
    private final int accentColor;

    public SnapSlider(int x, int y, int w, int h, String label, int min, int max, int value, int accentColor, IntConsumer onChange) {
        super(x, y, w, h, LiteralText.EMPTY, clamp01(min, max, value));
        this.min = min;
        this.max = max;
        this.label = label;
        this.onChange = onChange;
        this.accentColor = accentColor;
        updateMessage();
    }

    private static double clamp01(int min, int max, int value) {
        if (max == min) return 0;
        return Math.max(0.0, Math.min(1.0, (value - min) / (double) (max - min)));
    }

    public int getIntValue() {
        return min + (int) Math.round(this.value * (max - min));
    }

    @Override
    protected void updateMessage() {
        this.setMessage(new LiteralText(label + ": " + getIntValue()));
    }

    @Override
    protected void applyValue() {
        onChange.accept(getIntValue());
    }

    private void applySnappedValue(double mouseX) {
        double raw = (mouseX - this.x) / (double) this.width;
        int steps = max - min;
        double snapped = steps <= 0 ? raw : Math.round(raw * steps) / (double) steps;
        this.value = Math.max(0.0, Math.min(1.0, snapped));
        updateMessage();
        applyValue();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            applySnappedValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        applySnappedValue(mouseX);
        return true;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int trackX = this.x;
        int trackY = this.y + this.height / 2 - 3;
        int trackW = this.width;
        int trackH = 6;

        // Track: border + bg, then both corners on BOTH ends cut back to the panel color by the
        // same amount, so left and right read as identically rounded instead of one side looking
        // rounded and the other square.
        fill(matrices, trackX, trackY, trackX + trackW, trackY + trackH, DarkTheme.WIDGET_BORDER);
        fill(matrices, trackX + 1, trackY + 1, trackX + trackW - 1, trackY + trackH - 1, DarkTheme.WIDGET_BG);
        cutCorner(matrices, trackX, trackY);
        cutCorner(matrices, trackX + trackW - 1, trackY);
        cutCorner(matrices, trackX, trackY + trackH - 1);
        cutCorner(matrices, trackX + trackW - 1, trackY + trackH - 1);

        // XP-bar style progress fill from the track's left edge up to the handle.
        int usableWidth = this.width - HANDLE_WIDTH;
        int handleX = this.x + (int) Math.round(usableWidth * this.value);
        int fillEnd = Math.min(trackX + trackW - 1, handleX + HANDLE_WIDTH / 2);
        if (fillEnd > trackX + 1) {
            fill(matrices, trackX + 1, trackY + 1, fillEnd, trackY + trackH - 1, DarkTheme.PROGRESS_FILL);
        }

        // Tick marks at every snap step, clamped strictly inside the border (the last tick used
        // to land one pixel outside it on the right, which is what made the two ends look
        // asymmetric even though the border itself was symmetric).
        int steps = max - min;
        if (steps > 0 && steps <= 32) {
            for (int i = 0; i <= steps; i++) {
                int tx = trackX + 1 + (int) Math.round((trackW - 3) * (i / (double) steps));
                fill(matrices, tx, trackY + 1, tx + 1, trackY + trackH - 1, 0x50000000);
            }
        }

        // Handle: ranges across the full track width, so at value=0/1 it's flush with the
        // track's own left/right edges instead of stopping short.
        fill(matrices, handleX, this.y, handleX + HANDLE_WIDTH, this.y + this.height, DarkTheme.WIDGET_BORDER);
        fill(matrices, handleX + 1, this.y + 1, handleX + HANDLE_WIDTH - 1, this.y + this.height - 1, accentColor);

        RenderSystem.enableBlend();
        drawCenteredText(matrices, net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                this.getMessage(), this.x + this.width / 2, this.y + this.height + 1, DarkTheme.TEXT_DIM);
    }

    private static void cutCorner(MatrixStack matrices, int px, int py) {
        fill(matrices, px, py, px + 1, py + 1, DarkTheme.PANEL);
    }
}
