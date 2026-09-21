package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/**
 * Wide clickable row for a criterion's on/off toggle, replacing a small checkbox sitting next
 * to a static label - the whole row is the control, and its name text itself changes color
 * (dim gray off, green on) instead of a separate small indicator.
 */
public class SectionToggleButton extends ButtonWidget {
    private boolean checked;

    public SectionToggleButton(int x, int y, int w, int h, String label, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, w, h, new LiteralText(label), btn -> {
            SectionToggleButton self = (SectionToggleButton) btn;
            self.checked = !self.checked;
            onToggle.accept(self.checked);
        });
        this.checked = initial;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, DarkTheme.WIDGET_BORDER);
        int innerFill = checked ? 0xFF20301F : DarkTheme.WIDGET_BG;
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, innerFill);

        int textColor = checked ? DarkTheme.ACCENT : DarkTheme.TEXT_DIM;
        MinecraftClient client = MinecraftClient.getInstance();
        drawStringWithShadow(matrices, client.textRenderer, this.getMessage().getString(),
                this.x + 8, this.y + (this.height - 8) / 2, textColor);
    }
}
