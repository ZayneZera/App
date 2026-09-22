package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.MenuNav;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

/**
 * The seed bank's new entry point (replaces the old flat stack list): pick a top-level category -
 * Buried Treasure, Village, Ruined Portal, or OP (bonus multi-matches and Looting finds, see
 * SeedBankOpScreen) - before seeing any seeds. Each is an icon tile (CategoryIconButton) matching
 * CategoryStyle's item-per-category language used everywhere else in the seed bank, and routes
 * into a {@link SeedBankTableScreen} with that category's own column set (see SeedBankTabs).
 */
public class SeedBankCategoryScreen extends Screen {

    private final Screen parent;

    public SeedBankCategoryScreen(Screen parent) {
        super(new LiteralText("Seedbank"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();

        int tileW = 96, tileH = 72, gap = 10;
        int totalW = tileW * 4 + gap * 3;
        int x = this.width / 2 - totalW / 2;
        int y = this.height / 2 - tileH / 2;

        this.addButton(new CategoryIconButton(x, y, tileW, tileH, Items.CHEST, "Buried Treasure", b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, "Buried Treasure",
                        SeedBankTabs.buriedTreasureColumns(), SeedBankTabs::isBuriedTreasureTab))));
        x += tileW + gap;
        this.addButton(new CategoryIconButton(x, y, tileW, tileH, Items.BELL, "Dorf", b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, "Dorf",
                        SeedBankTabs.villageColumns(), SeedBankTabs::isVillageTab))));
        x += tileW + gap;
        this.addButton(new CategoryIconButton(x, y, tileW, tileH, Items.OBSIDIAN, "Ruined Portal", b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, "Ruined Portal",
                        SeedBankTabs.ruinedPortalColumns(), SeedBankTabs::isRuinedPortalTab))));
        x += tileW + gap;
        this.addButton(new CategoryIconButton(x, y, tileW, tileH, Items.NETHERITE_BLOCK, "OP", b ->
                MenuNav.navigate(this.client, new SeedBankOpScreen(this))));

        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height - 34, 150, 20,
                new LiteralText("Schließen"), b -> MenuNav.navigate(this.client, parent)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MenuNav.handleNavClick(this.client, button, () -> MenuNav.navigate(this.client, parent))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
