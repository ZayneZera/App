package com.zayne.seedfilter.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

import java.util.function.Consumer;

/**
 * Wide clickable row for a criterion's on/off toggle, replacing a small checkbox sitting next
 * to a static label - the whole row is the control. On/off reads from the row's own border color
 * (green = on, gray = off), with the name text itself also shifting color the same way; an
 * optional leading item icon identifies the category at a glance (Village=Bell,
 * RuinedPortal=Obsidian, BuriedTreasure=Chest, Bastion=Blackstone, Fortress=NetherBricks).
 */
public class SectionToggleButton extends ButtonWidget {
    private final Item icon; // null = no icon, text starts at the old left inset
    private boolean checked;

    public SectionToggleButton(int x, int y, int w, int h, String label, boolean initial, Consumer<Boolean> onToggle) {
        this(x, y, w, h, label, null, initial, onToggle);
    }

    public SectionToggleButton(int x, int y, int w, int h, String label, Item icon, boolean initial, Consumer<Boolean> onToggle) {
        super(x, y, w, h, new LiteralText(label), btn -> {
            SectionToggleButton self = (SectionToggleButton) btn;
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

        MinecraftClient client = MinecraftClient.getInstance();
        int textX = this.x + 8;
        if (icon != null) {
            int iconY = this.y + (this.height - 16) / 2;
            client.getItemRenderer().renderGuiItemIcon(new ItemStack(icon), this.x + 4, iconY);
            textX = this.x + 24;
        }

        int textColor = checked ? DarkTheme.ACCENT : DarkTheme.TEXT_DIM;
        drawStringWithShadow(matrices, client.textRenderer, this.getMessage().getString(),
                textX, this.y + (this.height - 8) / 2, textColor);
    }
}
