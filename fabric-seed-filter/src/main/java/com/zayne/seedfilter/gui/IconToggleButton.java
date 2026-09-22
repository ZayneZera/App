package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/**
 * Small square icon-only toggle, no label text - for a filter the player already knows by its
 * item (iron/diamond/etc.), so a name next to it would just be noise. On/off reads from the
 * border color (green = on, gray = off) rather than a separate checkmark, matching
 * SectionToggleButton's own on/off language at a smaller size.
 */
public class IconToggleButton extends ButtonWidget {
    public static final int SIZE = 18;

    private final Item icon;
    private boolean checked;

    public IconToggleButton(int x, int y, Item icon, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, SIZE, SIZE, LiteralText.EMPTY, btn -> {
            IconToggleButton self = (IconToggleButton) btn;
            self.checked = !self.checked;
            onToggle.accept(self.checked);
        });
        this.icon = icon;
        this.checked = initial;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int borderColor = checked ? DarkTheme.ACCENT : DarkTheme.WIDGET_BORDER;
        fill(matrices, this.x, this.y, this.x + this.width, this.y + this.height, borderColor);
        int innerFill = checked ? 0xFF20301F : DarkTheme.WIDGET_BG;
        fill(matrices, this.x + 1, this.y + 1, this.x + this.width - 1, this.y + this.height - 1, innerFill);

        ItemStack stack = new ItemStack(this.icon);
        MinecraftClient client = MinecraftClient.getInstance();
        int itemX = this.x + (this.width - 16) / 2;
        int itemY = this.y + (this.height - 16) / 2;
        client.getItemRenderer().renderGuiItemIcon(stack, itemX, itemY);
    }
}
