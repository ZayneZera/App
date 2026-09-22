package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.MenuNav;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * The seed bank's new entry point (replaces the old flat stack list): pick a top-level category -
 * Buried Treasure, Village, Ruined Portal, or OP (bonus multi-matches and Looting finds, see
 * SeedBankOpScreen) - before seeing any seeds. Each routes into a {@link SeedBankTableScreen}
 * with that category's own column set (see SeedBankTabs).
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

        int w = 180, h = 20, gap = 8;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - (h * 4 + gap * 3) / 2;

        this.addButton(new ButtonWidget(x, y, w, h, new LiteralText("Buried Treasure"), b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, "Buried Treasure",
                        SeedBankTabs.buriedTreasureColumns(), SeedBankTabs::isBuriedTreasureTab))));
        y += h + gap;
        this.addButton(new ButtonWidget(x, y, w, h, new LiteralText("Dorf"), b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, "Dorf",
                        SeedBankTabs.villageColumns(), SeedBankTabs::isVillageTab))));
        y += h + gap;
        this.addButton(new ButtonWidget(x, y, w, h, new LiteralText("Ruined Portal"), b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, "Ruined Portal",
                        SeedBankTabs.ruinedPortalColumns(), SeedBankTabs::isRuinedPortalTab))));
        y += h + gap;
        this.addButton(new ButtonWidget(x, y, w, h, new LiteralText("OP"), b ->
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
