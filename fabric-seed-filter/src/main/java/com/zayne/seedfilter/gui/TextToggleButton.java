package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/**
 * Small pill-shaped on/off toggle whose own label text is the state - "ODER" while on, "UND"
 * while off (or whatever two labels the caller passes), not a checkbox next to a fixed name.
 * Same green/gray border language as SectionToggleButton/IconToggleButton.
 */
public class TextToggleButton extends ButtonWidget {
    private final String offLabel;
    private final String onLabel;
    private boolean checked;

    public TextToggleButton(int x, int y, int w, int h, String offLabel, String onLabel, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, w, h, new LiteralText(initial ? onLabel : offLabel), btn -> {
            TextToggleButton self = (TextToggleButton) btn;
            self.checked = !self.checked;
            self.setMessage(new LiteralText(self.checked ? self.onLabel : self.offLabel));
            onToggle.accept(self.checked);
        });
        this.offLabel = offLabel;
        this.onLabel = onLabel;
        this.checked = initial;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int borderColor = checked ? DarkTheme.ACCENT : DarkTheme.WIDGET_BORDER;
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, borderColor);
        int innerFill = checked ? 0xFF20301F : DarkTheme.WIDGET_BG;
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, innerFill);

        int textColor = checked ? DarkTheme.ACCENT : DarkTheme.TEXT_DIM;
        MinecraftClient client = MinecraftClient.getInstance();
        drawCenteredText(matrices, client.textRenderer, this.getMessage(),
                this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }
}
