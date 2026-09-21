package com.zayne.seedfilter.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/**
 * Small square toggle: the box itself never changes fill/border, only whether a checkmark is
 * drawn over it (slightly overflowing the box edges) - originally-drawn geometry, not an
 * imported icon.
 */
public class DarkCheckbox extends ButtonWidget {
    private boolean checked;
    private final int checkColor;

    public DarkCheckbox(int x, int y, int size, boolean initial, int checkColor, Consumer<Boolean> onToggle) {
        super(x, y, size, size, LiteralText.EMPTY, btn -> {
            DarkCheckbox self = (DarkCheckbox) btn;
            self.checked = !self.checked;
            onToggle.accept(self.checked);
        });
        this.checked = initial;
        this.checkColor = checkColor;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, DarkTheme.WIDGET_BORDER);
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, DarkTheme.WIDGET_BG);
        if (checked) {
            drawCheckmark(matrices, this.x, this.y, this.width, this.height, checkColor);
        }
    }

    /** Simple two-stroke check shape, sized to the box and allowed to overflow it slightly. */
    private static void drawCheckmark(MatrixStack matrices, int x, int y, int w, int h, int color) {
        int cx = x - 1;
        int cy = y + h / 2;
        int shortLeg = Math.max(2, h / 3);
        for (int i = 0; i <= shortLeg; i++) {
            int px = cx + i;
            int py = cy + i;
            fill(matrices, px, py, px + 2, py + 2, color);
        }
        int sx = cx + shortLeg;
        int sy = cy + shortLeg;
        int longLeg = w;
        for (int i = 0; i <= longLeg; i++) {
            int px = sx + i;
            int py = sy - i;
            fill(matrices, px, py, px + 2, py + 2, color);
        }
    }
}
