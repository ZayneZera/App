package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

/**
 * A square tile button showing an item icon with its label underneath, and a highlighted border
 * on hover - used by SeedBankCategoryScreen for its BT/Village/RP/OP picker instead of plain text
 * buttons, so the category is recognizable by its item icon at a glance.
 */
public class CategoryIconButton extends ButtonWidget {

    private final Item icon;
    private final String label;

    public CategoryIconButton(int x, int y, int w, int h, Item icon, String label, PressAction onPress) {
        super(x, y, w, h, LiteralText.EMPTY, onPress);
        this.icon = icon;
        this.label = label;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        boolean hovered = this.active && this.isMouseOver(mouseX, mouseY);
        int borderColor = hovered ? DarkTheme.ACCENT : DarkTheme.WIDGET_BORDER;
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, borderColor);
        int innerColor = hovered ? 0xFF263426 : DarkTheme.WIDGET_BG;
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, innerColor);

        MinecraftClient client = MinecraftClient.getInstance();
        int itemX = this.x + (this.width - 16) / 2;
        int itemY = this.y + 10;
        client.getItemRenderer().renderGuiItemIcon(new ItemStack(icon), itemX, itemY);

        int textW = client.textRenderer.getWidth(label);
        int textX = this.x + (this.width - textW) / 2;
        int textY = this.y + this.height - 16;
        int textColor = hovered ? DarkTheme.TEXT : DarkTheme.TEXT_DIM;
        drawStringWithShadow(matrices, client.textRenderer, label, textX, textY, textColor);
    }
}
