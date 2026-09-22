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
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

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

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
