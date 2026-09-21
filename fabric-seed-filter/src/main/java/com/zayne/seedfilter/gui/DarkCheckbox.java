package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/** Small square toggle matching the dark-theme panel instead of vanilla's checkbox texture. */
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
        if (checked) {
            fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, accentColor);
        } else {
            fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, 0xFF322F2B);
            fill(matrices, this.x, this.y, this.x + this.width, this.y + 1, 0xFF57534A);
            fill(matrices, this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, 0xFF57534A);
            fill(matrices, this.x, this.y, this.x + 1, this.y + this.height, 0xFF57534A);
            fill(matrices, this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, 0xFF57534A);
        }
        if (checked) {
            MinecraftClient client = MinecraftClient.getInstance();
            drawCenteredText(matrices, client.textRenderer, new LiteralText("v"),
                    this.x + this.width / 2, this.y + (this.height - 8) / 2, 0xFF262420);
        }
    }
}
