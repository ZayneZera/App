package com.zayne.seedfilter.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Small square toggle: the box itself never changes fill/border, only whether the checkmark
 * icon is drawn over it (slightly overflowing the box edges).
 */
public class DarkCheckbox extends ButtonWidget {
    private static final Identifier CHECKMARK = new Identifier("seedfilter", "textures/gui/checkmark.png");

    private boolean checked;

    public DarkCheckbox(int x, int y, int size, boolean initial, int unusedAccent, Consumer<Boolean> onToggle) {
        super(x, y, size, size, LiteralText.EMPTY, btn -> {
            DarkCheckbox self = (DarkCheckbox) btn;
            self.checked = !self.checked;
            onToggle.accept(self.checked);
        });
        this.checked = initial;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, DarkTheme.WIDGET_BORDER);
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, DarkTheme.WIDGET_BG);
        if (checked) {
            int size = this.height + 6;
            int dx = this.x + this.width / 2 - size / 2;
            int dy = this.y + this.height / 2 - size / 2;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, 1f);
            MinecraftClient.getInstance().getTextureManager().bindTexture(CHECKMARK);
            drawTexture(matrices, dx, dy, size, size, 0f, 0f, 24, 24, 24, 24);
        }
    }
}
