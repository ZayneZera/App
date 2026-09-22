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
 * icon is drawn over it. checkmark.png is NOT square (13x10) - it's drawn at its own native
 * pixel size, centered over the box, with no scaling applied at all (CHECK_W/CHECK_H must match
 * the actual file's pixel dimensions exactly, or the texture gets resampled and looks mangled).
 */
public class DarkCheckbox extends ButtonWidget {
    private static final Identifier CHECKMARK = new Identifier("seedfilter", "textures/gui/checkmark.png");
    public static final int BOX_SIZE = 12;
    private static final int CHECK_W = 13;
    private static final int CHECK_H = 10;

    private boolean checked;

    public DarkCheckbox(int x, int y, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, BOX_SIZE, BOX_SIZE, LiteralText.EMPTY, btn -> {
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
            int dx = this.x + (this.width - CHECK_W) / 2;
            int dy = this.y + (this.height - CHECK_H) / 2;

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, 1f);
            MinecraftClient.getInstance().getTextureManager().bindTexture(CHECKMARK);
            drawTexture(matrices, dx, dy, CHECK_W, CHECK_H, 0f, 0f, CHECK_W, CHECK_H, CHECK_W, CHECK_H);
        }
    }
}
