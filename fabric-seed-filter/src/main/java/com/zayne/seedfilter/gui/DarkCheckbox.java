package com.zayne.seedfilter.gui;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/** Small square toggle: only the fill changes between on/off, the border always stays the same. */
public class DarkCheckbox extends ButtonWidget {
    private boolean checked;
    private final int accentColor;

    public DarkCheckbox(int x, int y, int size, boolean initial, int accentColor, Consumer<Boolean> onToggle) {
        super(x, y, size, size, LiteralText.EMPTY, btn -> {
            DarkCheckbox self = (DarkCheckbox) btn;
            self.checked = !self.checked;
            onToggle.accept(self.checked);
        });
        this.checked = initial;
        this.accentColor = accentColor;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int fillColor = checked ? accentColor : DarkTheme.WIDGET_BG;
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, DarkTheme.WIDGET_BORDER);
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, fillColor);
    }
}
