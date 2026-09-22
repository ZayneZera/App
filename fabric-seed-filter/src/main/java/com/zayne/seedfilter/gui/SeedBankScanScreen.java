package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.EngineStats;
import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.MenuNav;
import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.SeedBankEntry;
import com.zayne.seedfilter.SeedFilterConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opened by the title screen's chest-icon button (see MixinTitleScreen) - the seed bank's "find
 * and save, don't join" mode. Runs seedfilter.exe --search-bank in the background indefinitely
 * (see ExternalEngine.runBankSearch), saving every match to the seed bank as it's found rather
 * than joining any of them, until Abbrechen is pressed. Never touches the world/player at all, so
 * unlike CountdownScreen/ScanProgressScreen there's no disconnect/rejoin dance here.
 */
public class SeedBankScanScreen extends Screen {

    private final Screen parent;
    private final AtomicReference<Process> processHolder = new AtomicReference<>();
    private final EngineStats stats = new EngineStats();
    private final SeedBank bank;
    private final SeedFilterConfig config;

    private final AtomicInteger totalFound = new AtomicInteger();
    private final AtomicInteger opFound = new AtomicInteger();
    private final AtomicInteger[] perCategoryFound = new AtomicInteger[]{
            new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger()
    };

    private volatile boolean stopped = false;

    public SeedBankScanScreen(Screen parent) {
        super(new LiteralText("Seedbank-Suche"));
        this.parent = parent;
        this.config = SeedFilterConfig.load(ExternalEngine.getConfigPath());
        this.bank = SeedBank.load(ExternalEngine.getSeedBankPath());

        ExternalEngine.runBankSearch(processHolder, stats, this::onMatch, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> stopped = true);
        });
    }

    /** Called from the background reader thread (see ExternalEngine.runBankSearch) - the actual
     * save + counter updates are handed to the client thread since SeedBank isn't otherwise
     * synchronized and this screen's fields are read from render() on the client thread. */
    private void onMatch(ExternalEngine.Result result) {
        SeedBankEntry entry = SeedBankEntry.fromResult(result, config);
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            bank.add(entry);
            totalFound.incrementAndGet();
            if (ExternalEngine.isOp(entry.matchedCategories)) {
                opFound.incrementAndGet();
            }
            for (int i = 0; i < CategoryStyle.BITS.length; i++) {
                if ((entry.matchedCategories & CategoryStyle.BITS[i]) != 0) {
                    perCategoryFound[i].incrementAndGet();
                }
            }
        });
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 130, 150, 20,
                new LiteralText("Abbrechen"), button -> {
            ExternalEngine.cancel(processHolder);
            this.client.openScreen(this.parent);
        }));
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 154, 150, 20,
                new LiteralText("Seedbank ansehen"), button ->
                        MenuNav.navigate(this.client, new SeedBankCategoryScreen(this))));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Versuche: " + stats.attempts.get()),
                this.width / 2, this.height / 2 - 40, DarkTheme.TEXT_DIM);

        String status = stopped ? "Gestoppt" : "Sucht weiter...";
        drawCenteredText(matrices, this.textRenderer, new LiteralText(status),
                this.width / 2, this.height / 2 - 26, stopped ? DarkTheme.TEXT_DIM : DarkTheme.ACCENT);

        int y = this.height / 2 - 4;
        drawCenteredText(matrices, this.textRenderer,
                new LiteralText("Gefunden: " + totalFound.get()),
                this.width / 2, y, DarkTheme.TEXT);
        y += 14;

        MinecraftClient client = MinecraftClient.getInstance();
        if (opFound.get() > 0) {
            String text = "OP: " + opFound.get();
            int textW = this.textRenderer.getWidth(text);
            int rowX = this.width / 2 - (textW + 20) / 2;
            client.getItemRenderer().renderGuiItemIcon(new ItemStack(CategoryStyle.OP_ICON), rowX, y - 4);
            drawStringWithShadow(matrices, this.textRenderer, text, rowX + 20, y, 0xFFD700);
            y += 13;
        }
        for (int i = 0; i < CategoryStyle.LABELS.length; i++) {
            int count = perCategoryFound[i].get();
            if (count == 0) continue;
            String text = CategoryStyle.LABELS[i] + ": " + count;
            int textW = this.textRenderer.getWidth(text);
            int rowX = this.width / 2 - (textW + 20) / 2;
            client.getItemRenderer().renderGuiItemIcon(new ItemStack(CategoryStyle.ICONS[i]), rowX, y - 4);
            drawStringWithShadow(matrices, this.textRenderer, text, rowX + 20, y, 0x77AAFF);
            y += 13;
        }

        renderFilterPanel(matrices);

        super.render(matrices, mouseX, mouseY, delta);
    }

    /** Left-side "what is this search actually doing" panel - every enabled category with its
     * icon and key settings, the AND/OR mode, and a note about the always-on Looting side channel
     * (see engine.c's find_looting_ruined_portal), so the settings that produced whatever shows up
     * in the seed bank are visible right here instead of needing to reopen the settings menu. */
    private void renderFilterPanel(MatrixStack matrices) {
        List<Object[]> rows = new ArrayList<>(); // {Item icon (nullable), String text, int color}
        rows.add(new Object[]{null, "Aktive Filter", DarkTheme.HEADING});
        rows.add(new Object[]{null, config.orMode ? "Modus: ODER" : "Modus: UND", DarkTheme.TEXT_DIM});

        if (config.villageEnabled) {
            rows.add(new Object[]{Items.BELL, "Dorf: " + config.villageMaxChunks + "C", DarkTheme.TEXT});
        }
        if (config.ruinedPortalEnabled) {
            String extra = config.ruinedPortalLootingSword ? " (Loot)" : "";
            rows.add(new Object[]{Items.OBSIDIAN, "Ruined Portal: " + config.ruinedPortalMaxChunks + "C" + extra, DarkTheme.TEXT});
        }
        if (config.buriedTreasureEnabled) {
            StringBuilder sb = new StringBuilder("Buried Treasure: ").append(config.buriedTreasureMaxChunks).append("C");
            if (config.buriedTreasureMinTnt > 0) sb.append(", TNT").append(config.buriedTreasureMinTnt >= 2 ? "2+" : config.buriedTreasureMinTnt);
            if (config.buriedTreasureDiamondFilter) sb.append(", Dia");
            if (config.buriedTreasureIronFilter) sb.append(", Iron");
            rows.add(new Object[]{Items.CHEST, sb.toString(), DarkTheme.TEXT});
        }
        if (config.bastionEnabled) {
            StringBuilder sb = new StringBuilder("Bastion: ").append(config.bastionMaxNetherChunks).append("C ");
            if (config.bastionAllowBridge) sb.append("B");
            if (config.bastionAllowHousing) sb.append("H");
            if (config.bastionAllowStables) sb.append("S");
            if (config.bastionAllowTreasure) sb.append("K");
            rows.add(new Object[]{Items.BLACKSTONE, sb.toString(), DarkTheme.TEXT});
        }
        if (config.fortressEnabled) {
            rows.add(new Object[]{Items.NETHER_BRICKS, "Fortress: " + config.fortressMaxNetherChunks + "C", DarkTheme.TEXT});
        }
        rows.add(new Object[]{CategoryStyle.LOOTING_ICON, "Looting II+: 8C (immer)", DarkTheme.TEXT_DIM});

        int panelX = 16;
        int panelY = 40;
        int panelW = 200;
        int rowH = 14;
        int panelH = rows.size() * rowH + 10;

        fill(matrices, panelX, panelY, panelX + panelW, panelY + panelH, DarkTheme.BORDER);
        fill(matrices, panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH - 1, DarkTheme.PANEL);

        MinecraftClient client = MinecraftClient.getInstance();
        int ty = panelY + 6;
        for (Object[] row : rows) {
            Item icon = (Item) row[0];
            String text = (String) row[1];
            int color = (int) row[2];
            int textX = panelX + 8;
            if (icon != null) {
                client.getItemRenderer().renderGuiItemIcon(new ItemStack(icon), panelX + 6, ty - 2);
                textX = panelX + 24;
            }
            drawStringWithShadow(matrices, this.textRenderer, text, textX, ty, color);
            ty += rowH;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
