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
        // Track background. Border must be inset on ALL sides (including left/right) or the
        // ends look open/cut off.
        fill(matrices, this.x, this.y + this.height / 2 - 2, this.x + this.width, this.y + this.height / 2 + 2, DarkTheme.WIDGET_BORDER);
        fill(matrices, this.x + 1, this.y + this.height / 2 - 1, this.x + this.width - 1, this.y + this.height / 2 + 1, DarkTheme.WIDGET_BG);

        // Tick marks at every snap step (only drawn if they wouldn't be pixel-mush - cap density).
        int steps = max - min;
        if (steps > 0 && steps <= 32) {
            for (int i = 0; i <= steps; i++) {
                int tx = this.x + (int) (this.width * (i / (double) steps));
                fill(matrices, tx, this.y + this.height / 2 - 1, tx + 1, this.y + this.height / 2 + 1, 0xA0888888);
            }
        }

        // Handle: ranges across the full track width, so at value=0/1 it's flush with the
        // track's own left/right edges instead of stopping short.
        int usableWidth = this.width - HANDLE_WIDTH;
        int handleX = this.x + (int) Math.round(usableWidth * this.value);
        fill(matrices, handleX, this.y + 2, handleX + HANDLE_WIDTH, this.y + this.height - 2, DarkTheme.WIDGET_BORDER);
        fill(matrices, handleX + 1, this.y + 3, handleX + HANDLE_WIDTH - 1, this.y + this.height - 3, accentColor);

        RenderSystem.enableBlend();
        drawCenteredText(matrices, net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                this.getMessage(), this.x + this.width / 2, this.y + this.height + 1, DarkTheme.TEXT_DIM);
    }
}
