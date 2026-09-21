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
 * icon is drawn over it. BOX_SIZE is drawn 1:1 against checkmark.png's own pixel size (no
 * scaling at all) - checkmark.png MUST be exactly BOX_SIZE x BOX_SIZE pixels, or the texture
 * gets resampled again and looks mangled the same way it did before.
 */
public class DarkCheckbox extends ButtonWidget {
    private static final Identifier CHECKMARK = new Identifier("seedfilter", "textures/gui/checkmark.png");
    public static final int BOX_SIZE = 12;

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
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1f, 1f, 1f, 1f);
            MinecraftClient.getInstance().getTextureManager().bindTexture(CHECKMARK);
            drawTexture(matrices, this.x, this.y, BOX_SIZE, BOX_SIZE, 0f, 0f, BOX_SIZE, BOX_SIZE, BOX_SIZE, BOX_SIZE);
        }
    }
}
