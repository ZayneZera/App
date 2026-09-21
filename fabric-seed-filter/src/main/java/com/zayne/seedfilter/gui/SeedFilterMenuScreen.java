package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.SeedFilterConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * In-game settings menu (Ctrl+M), replacing the old external Win32 GUI window - writes the
 * same seedfilter.cfg the exe's headless search reads, styled as a dark, rounded-corner panel
 * instead of vanilla GUI widgets.
 */
public class SeedFilterMenuScreen extends Screen {

    private final Screen background;
    private final SeedFilterConfig config;
    private int scrollOffset = 0;
    private int contentHeight = 0;

    private int panelX, panelY, panelW, panelH;
    private int nextY;

    private final List<Object[]> sectionLabels = new ArrayList<>();
    private final List<Object[]> checkboxLabels = new ArrayList<>();

    public SeedFilterMenuScreen(Screen background) {
        super(new LiteralText("Seed-Filter"));
        this.background = background;
        this.config = SeedFilterConfig.load(ExternalEngine.getConfigPath());
    }

    public Screen getBackgroundScreen() {
        return background;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        sectionLabels.clear();
        checkboxLabels.clear();

        panelW = Math.min(360, this.width - 24);
        panelH = Math.min(this.height - 24, 460);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int contentX = panelX + 16;
        int contentW = panelW - 32;
        nextY = panelY + 14 - scrollOffset;

        addSection("Ruined Portal", contentX, config.ruinedPortalEnabled, v -> config.ruinedPortalEnabled = v);
        addCheckboxRow(contentX, "Looting", config.ruinedPortalLootingSword, v -> config.ruinedPortalLootingSword = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.ruinedPortalMaxChunks, v -> config.ruinedPortalMaxChunks = v);

        addSection("Village", contentX, config.villageEnabled, v -> config.villageEnabled = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.villageMaxChunks, v -> config.villageMaxChunks = v);

        addSection("Buried Treasure", contentX, config.buriedTreasureEnabled, v -> config.buriedTreasureEnabled = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.buriedTreasureMaxChunks, v -> config.buriedTreasureMaxChunks = v);

        addSection("Bastion", contentX, config.bastionEnabled, v -> config.bastionEnabled = v);
        addCheckboxRow(contentX, "Bridge", config.bastionAllowBridge, v -> config.bastionAllowBridge = v);
        addCheckboxRow(contentX, "Housing", config.bastionAllowHousing, v -> config.bastionAllowHousing = v);
        addCheckboxRow(contentX, "Stables", config.bastionAllowStables, v -> config.bastionAllowStables = v);
        addCheckboxRow(contentX, "Treasure", config.bastionAllowTreasure, v -> config.bastionAllowTreasure = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.bastionMaxNetherChunks, v -> config.bastionMaxNetherChunks = v);

        addSection("Fortress", contentX, config.fortressEnabled, v -> config.fortressEnabled = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.fortressMaxNetherChunks, v -> config.fortressMaxNetherChunks = v);

        nextY += 6;
        addCheckboxRow(contentX, "Cheats aktivieren", config.enableCheats, v -> config.enableCheats = v);

        contentHeight = (nextY - (panelY + 14 - scrollOffset)) + 10;
    }

    private void addSection(String name, int x, boolean initial, Consumer<Boolean> setter) {
        this.addButton(new DarkCheckbox(x, nextY, 12, initial, DarkTheme.ACCENT, setter));
        sectionLabels.add(new Object[]{name, x + 18, nextY + 2});
        nextY += 20;
    }

    private void addCheckboxRow(int x, String label, boolean initial, Consumer<Boolean> setter) {
        this.addButton(new DarkCheckbox(x + 4, nextY, 10, initial, DarkTheme.ACCENT, setter));
        checkboxLabels.add(new Object[]{label, x + 20, nextY + 1});
        nextY += 17;
    }

    private void addSliderRow(int x, int w, String label, int min, int max, int value, IntConsumer setter) {
        this.addButton(new SnapSlider(x, nextY, w, 12, label, min, max, value, DarkTheme.ACCENT, setter));
        nextY += 26;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        if (background != null) {
            background.render(matrices, -1, -1, delta);
            fill(matrices, 0, 0, this.width, this.height, 0x66000000);
        }

        fillRounded(matrices, panelX, panelY, panelW, panelH, DarkTheme.PANEL, DarkTheme.PANEL_BORDER);

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, panelY - 14, DarkTheme.TEXT);

        for (Object[] entry : sectionLabels) {
            int y = (int) entry[2];
            if (y < panelY || y > panelY + panelH - 10) continue;
            drawStringWithShadow(matrices, this.textRenderer, (String) entry[0], (int) entry[1], y, DarkTheme.TEXT);
        }
        for (Object[] entry : checkboxLabels) {
            int y = (int) entry[2];
            if (y < panelY || y > panelY + panelH - 10) continue;
            drawStringWithShadow(matrices, this.textRenderer, (String) entry[0], (int) entry[1], y, DarkTheme.TEXT_DIM);
        }

        super.render(matrices, mouseX, mouseY, delta);

        drawCenteredText(matrices, this.textRenderer, new LiteralText("Strg+M zum Schließen"),
                this.width / 2, panelY + panelH + 8, DarkTheme.TEXT_DIM);
    }

    private static void fillRounded(MatrixStack matrices, int x, int y, int w, int h, int fillColor, int borderColor) {
        // 2px corner notches approximate rounding within this version's plain-rect renderer.
        fill(matrices, x + 2, y, x + w - 2, y + h, borderColor);
        fill(matrices, x, y + 2, x + w, y + h - 2, borderColor);
        fill(matrices, x + 3, y + 1, x + w - 3, y + h - 1, fillColor);
        fill(matrices, x + 1, y + 3, x + w - 1, y + h - 3, fillColor);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int visibleHeight = panelH - 24;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        int newOffset = this.scrollOffset - (int) (amount * 14);
        this.scrollOffset = Math.max(0, Math.min(newOffset, maxScroll));
        this.init(this.client, this.width, this.height);
        return true;
    }

    @Override
    public void removed() {
        config.save(ExternalEngine.getConfigPath());
        super.removed();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
