package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.SeedFilterConfig;
import com.zayne.seedfilter.mixin.AbstractButtonWidgetAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * In-game settings menu (Ctrl+M) - writes the same seedfilter.cfg the exe's headless search
 * reads, styled as a dark, rounded-corner panel instead of vanilla GUI widgets. Deliberately
 * does NOT render whatever screen was open underneath (title screen, pause menu, ...) - vanilla
 * button text was bleeding through the panel when we tried that, since Minecraft's 2D GUI has
 * no real depth buffer to rely on for draw order. The standard renderBackground() dim/blur is
 * enough context on its own.
 */
public class SeedFilterMenuScreen extends Screen {

    /** Section header row height - taller than a plain icon toggle so a block-textured icon
     * (Obsidian/Blackstone/Nether Bricks) doesn't look cramped against the row's edges. */
    private static final int SECTION_H = 20;
    private static final int ICON_STEP = IconToggleButton.SIZE + 6;

    private final Screen background;
    private final SeedFilterConfig config;
    private int scrollOffset = 0;
    private int contentHeight = 0;

    private int panelX, panelY, panelW, panelH;
    private int nextY;

    private final List<Object[]> checkboxLabels = new ArrayList<>();
    private final List<Integer> dividerYs = new ArrayList<>();
    /** Static (non-clickable) item icons drawn during render() - e.g. the TNT icon next to its
     * slider, which isn't itself a toggle (the slider's own 0 value already means "off"). */
    private final List<Object[]> staticIcons = new ArrayList<>();

    public SeedFilterMenuScreen(Screen background) {
        super(LiteralText.EMPTY);
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
        checkboxLabels.clear();
        dividerYs.clear();
        staticIcons.clear();

        panelW = Math.min(360, this.width - 24);
        panelH = Math.min(this.height - 24, 460);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int contentX = panelX + 16;
        int contentW = panelW - 32;
        nextY = panelY + 14 - scrollOffset;

        this.addButton(new TextToggleButton(contentX, nextY, 70, 18, "UND", "ODER", config.orMode, v -> config.orMode = v));
        nextY += 18 + 6;
        addDivider(contentX, contentW);

        addSection("Ruined Portal", Items.OBSIDIAN, contentX, contentW, config.ruinedPortalEnabled, v -> config.ruinedPortalEnabled = v);
        // Looting-sword and frame-check: icon-only toggles side by side, no label text.
        this.addButton(new IconToggleButton(contentX, nextY, Items.GOLDEN_SWORD, config.ruinedPortalLootingSword, v -> config.ruinedPortalLootingSword = v));
        this.addButton(new IconToggleButton(contentX + ICON_STEP, nextY, Items.CRYING_OBSIDIAN, config.ruinedPortalFrameCheck, v -> config.ruinedPortalFrameCheck = v));
        nextY += ICON_STEP;
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.ruinedPortalMaxChunks, v -> config.ruinedPortalMaxChunks = v);
        addDivider(contentX, contentW);

        addSection("Village", Items.BELL, contentX, contentW, config.villageEnabled, v -> config.villageEnabled = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.villageMaxChunks, v -> config.villageMaxChunks = v);
        addDivider(contentX, contentW);

        addSection("Buried Treasure", Items.CHEST, contentX, contentW, config.buriedTreasureEnabled, v -> config.buriedTreasureEnabled = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.buriedTreasureMaxChunks, v -> config.buriedTreasureMaxChunks = v);

        // Diamond/Iron/TNT all in one row: icon-only toggles for the first two (no label - the
        // icon already says what it is), TNT gets its icon plus a much smaller slider right next
        // to it (0-2 range, a full-width slider would be mostly empty track for 3 possible values).
        this.addButton(new IconToggleButton(contentX, nextY, Items.DIAMOND, config.buriedTreasureDiamondFilter, v -> config.buriedTreasureDiamondFilter = v));
        this.addButton(new IconToggleButton(contentX + ICON_STEP, nextY, Items.IRON_INGOT, config.buriedTreasureIronFilter, v -> config.buriedTreasureIronFilter = v));
        staticIcons.add(new Object[]{Items.TNT, contentX + ICON_STEP * 2, nextY + 1});
        this.addButton(new SnapSlider(contentX + ICON_STEP * 2 + 22, nextY + 4, 70, 12, "TNT", 0, 2, config.buriedTreasureMinTnt, DarkTheme.ACCENT, v -> config.buriedTreasureMinTnt = v));
        nextY += ICON_STEP;

        addSliderRow(contentX, contentW, "Min Fisch", 4, 8, config.buriedTreasureMinFish, v -> config.buriedTreasureMinFish = v);
        addDivider(contentX, contentW);

        addSection("Bastion", Items.BLACKSTONE, contentX, contentW, config.bastionEnabled, v -> config.bastionEnabled = v);
        // Bridge/Housing/Stables/Treasure: icon-only toggles side by side, no label text.
        this.addButton(new IconToggleButton(contentX, nextY, Items.LODESTONE, config.bastionAllowBridge, v -> config.bastionAllowBridge = v));
        this.addButton(new IconToggleButton(contentX + ICON_STEP, nextY, Items.BLACK_BED, config.bastionAllowHousing, v -> config.bastionAllowHousing = v));
        this.addButton(new IconToggleButton(contentX + ICON_STEP * 2, nextY, Items.SADDLE, config.bastionAllowStables, v -> config.bastionAllowStables = v));
        this.addButton(new IconToggleButton(contentX + ICON_STEP * 3, nextY, Items.GOLD_BLOCK, config.bastionAllowTreasure, v -> config.bastionAllowTreasure = v));
        nextY += ICON_STEP;
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.bastionMaxNetherChunks, v -> config.bastionMaxNetherChunks = v);
        addDivider(contentX, contentW);

        addSection("Fortress", Items.NETHER_BRICKS, contentX, contentW, config.fortressEnabled, v -> config.fortressEnabled = v);
        addSliderRow(contentX, contentW, "Chunks", 1, 32, config.fortressMaxNetherChunks, v -> config.fortressMaxNetherChunks = v);
        addDivider(contentX, contentW);

        addCheckboxRow(contentX, "Cheats aktivieren", config.enableCheats, v -> config.enableCheats = v);
        addCheckboxRow(contentX, "Creative aktivieren", config.creativeMode, v -> config.creativeMode = v);

        contentHeight = (nextY - (panelY + 14 - scrollOffset)) + 10;
    }

    private void addSection(String name, Item icon, int x, int w, boolean initial, Consumer<Boolean> setter) {
        this.addButton(new SectionToggleButton(x, nextY, w, SECTION_H, name, icon, initial, setter));
        nextY += SECTION_H + 4;
    }

    private void addCheckboxRow(int x, String label, boolean initial, Consumer<Boolean> setter) {
        this.addButton(new DarkCheckbox(x + 4, nextY, initial, setter));
        checkboxLabels.add(new Object[]{label, x + 22, nextY + 2});
        nextY += 16;
    }

    private void addSliderRow(int x, int w, String label, int min, int max, int value, IntConsumer setter) {
        this.addButton(new SnapSlider(x, nextY, w, 12, label, min, max, value, DarkTheme.ACCENT, setter));
        nextY += 23;
    }

    private void addDivider(int x, int w) {
        nextY += 3;
        dividerYs.add(nextY);
        nextY += 6;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        fillRounded(matrices, panelX, panelY, panelW, panelH, DarkTheme.PANEL, DarkTheme.BORDER);

        // Scrolling only moves each row's own Y (see init()'s "panelY + 14 - scrollOffset"
        // starting point) - widgets themselves have no idea about the panel's visible bounds, so
        // one scrolled above/below it stayed fully clickable even once its label (drawn below,
        // with its own bounds check) had already stopped rendering. Hide (which also disables
        // click handling, since AbstractButtonWidget's own mouseClicked checks this same field)
        // anything outside the visible content band before letting vanilla render/handle input.
        int visibleTop = panelY + 10;
        int visibleBottom = panelY + panelH - 10;
        for (AbstractButtonWidget widget : this.buttons) {
            AbstractButtonWidgetAccessor accessor = (AbstractButtonWidgetAccessor) widget;
            int y = accessor.getY();
            accessor.setVisible(y >= visibleTop && y <= visibleBottom);
        }

        for (int y : dividerYs) {
            if (y < visibleTop || y > visibleBottom) continue;
            fill(matrices, panelX + 12, y, panelX + panelW - 12, y + 1, DarkTheme.BORDER);
        }

        for (Object[] entry : checkboxLabels) {
            int y = (int) entry[2];
            if (y < visibleTop || y > visibleBottom) continue;
            drawStringWithShadow(matrices, this.textRenderer, (String) entry[0], (int) entry[1], y, DarkTheme.TEXT_DIM);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        for (Object[] entry : staticIcons) {
            int y = (int) entry[2];
            if (y < visibleTop || y > visibleBottom) continue;
            client.getItemRenderer().renderGuiItemIcon(new ItemStack((Item) entry[0]), (int) entry[1], y);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    /** Plain rectangle, sharp corners - gave up on faking rounded corners with flat fills. */
    private static void fillRounded(MatrixStack matrices, int x, int y, int w, int h, int fillColor, int borderColor) {
        fill(matrices, x, y, x + w, y + h, borderColor);
        fill(matrices, x + 1, y + 1, x + w - 1, y + h - 1, fillColor);
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
