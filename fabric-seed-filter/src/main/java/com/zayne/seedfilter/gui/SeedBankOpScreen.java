package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.MenuNav;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/**
 * OP's own sub-tab picker, reached from {@link SeedBankCategoryScreen}'s OP button: which bonus
 * combination (2-3 of Village/RuinedPortal/BuriedTreasure at once) or a Looting find (see
 * SeedBankTabs.isLooting) to look at, each its own {@link SeedBankTableScreen}.
 */
public class SeedBankOpScreen extends Screen {

    private final Screen parent;

    public SeedBankOpScreen(Screen parent) {
        super(new LiteralText("OP"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();

        int w = 220, h = 20, gap = 8;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - (h * 5 + gap * 4) / 2;

        y = addTab(x, y, w, h, gap, "Buried Treasure + Ruined Portal",
                SeedBankTabs.combinedColumns(true, true, false), SeedBankTabs::isBtRp);
        y = addTab(x, y, w, h, gap, "Buried Treasure + Dorf",
                SeedBankTabs.combinedColumns(true, false, true), SeedBankTabs::isBtVillage);
        y = addTab(x, y, w, h, gap, "Ruined Portal + Dorf",
                SeedBankTabs.combinedColumns(false, true, true), SeedBankTabs::isRpVillage);
        y = addTab(x, y, w, h, gap, "Buried Treasure + Ruined Portal + Dorf",
                SeedBankTabs.combinedColumns(true, true, true), SeedBankTabs::isBtRpVillage);
        addTab(x, y, w, h, gap, "Looting", SeedBankTabs.lootingColumns(), SeedBankTabs::isLooting);

        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height - 34, 150, 20,
                new LiteralText("Zurück"), b -> MenuNav.navigate(this.client, parent)));
    }

    private int addTab(int x, int y, int w, int h, int gap, String label,
                        java.util.List<SeedBankColumns.Column> columns,
                        java.util.function.Predicate<com.zayne.seedfilter.SeedBank.StackKey> filter) {
        this.addButton(new ButtonWidget(x, y, w, h, new LiteralText(label), b ->
                MenuNav.navigate(this.client, new SeedBankTableScreen(this, label, columns, filter))));
        return y + h + gap;
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
