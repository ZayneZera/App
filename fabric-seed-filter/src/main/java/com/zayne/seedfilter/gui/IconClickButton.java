package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

/**
 * Small square icon-only button with independent left/right click actions - e.g. the seed bank
 * table's Load (Enderpearl, left-click) and Delete (Lava Bucket, left-click = clear stack,
 * right-click = view history) row buttons. Generalizes MixinTitleScreen's private IconButton.
 */
public class IconClickButton extends ButtonWidget {
    public static final int SIZE = 18;

    private final Item icon;
    private final Runnable onRightClick;

    public IconClickButton(int x, int y, Item icon, Runnable onLeftClick, Runnable onRightClick) {
        super(x, y, SIZE, SIZE, LiteralText.EMPTY, btn -> onLeftClick.run());
        this.icon = icon;
        this.onRightClick = onRightClick;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            if (onRightClick != null) {
                onRightClick.run();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int borderColor = this.isMouseOver(mouseX, mouseY) ? DarkTheme.ACCENT : DarkTheme.WIDGET_BORDER;
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, borderColor);
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, DarkTheme.WIDGET_BG);

        ItemStack stack = new ItemStack(this.icon);
        MinecraftClient client = MinecraftClient.getInstance();
        int itemX = this.x + (this.width - 16) / 2;
        int itemY = this.y + (this.height - 16) / 2;
        client.getItemRenderer().renderGuiItemIcon(stack, itemX, itemY);
    }
}
