package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.EngineStats;
import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.SeedBankEntry;
import com.zayne.seedfilter.SeedFilterConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
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
    private final String signature;
    private final boolean cheats;
    private final boolean creative;

    private final AtomicInteger totalFound = new AtomicInteger();
    private final AtomicInteger opFound = new AtomicInteger();
    private final AtomicInteger[] perCategoryFound = new AtomicInteger[]{
            new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger()
    };
    private static final int[] CATEGORY_BITS = {
            ExternalEngine.CATEGORY_VILLAGE, ExternalEngine.CATEGORY_RUINED_PORTAL,
            ExternalEngine.CATEGORY_TREASURE, ExternalEngine.CATEGORY_BASTION, ExternalEngine.CATEGORY_FORTRESS
    };
    private static final String[] CATEGORY_LABELS = {"Dorf", "Ruined Portal", "Buried Treasure", "Bastion", "Fortress"};

    private volatile boolean stopped = false;

    public SeedBankScanScreen(Screen parent) {
        super(new LiteralText("Seedbank-Suche"));
        this.parent = parent;
        SeedFilterConfig config = SeedFilterConfig.load(ExternalEngine.getConfigPath());
        this.bank = SeedBank.load(ExternalEngine.getSeedBankPath());
        this.signature = SeedBank.buildSignature(config);
        this.cheats = config.enableCheats;
        this.creative = config.creativeMode;

        ExternalEngine.runBankSearch(processHolder, stats, this::onMatch, () -> {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> stopped = true);
        });
    }

    /** Called from the background reader thread (see ExternalEngine.runBankSearch) - the actual
     * save + counter updates are handed to the client thread since SeedBank isn't otherwise
     * synchronized and this screen's fields are read from render() on the client thread. */
    private void onMatch(ExternalEngine.Result result) {
        SeedBankEntry entry = SeedBankEntry.fromResult(result, signature, cheats, creative);
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            bank.add(entry);
            totalFound.incrementAndGet();
            if (Integer.bitCount(entry.matchedCategories) >= 2) {
                opFound.incrementAndGet();
            }
            for (int i = 0; i < CATEGORY_BITS.length; i++) {
                if ((entry.matchedCategories & CATEGORY_BITS[i]) != 0) {
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
                        this.client.openScreen(new SeedBankBrowseScreen(this))));
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
                new LiteralText("Gefunden: " + totalFound.get() + (opFound.get() > 0 ? "  (davon OP: " + opFound.get() + ")" : "")),
                this.width / 2, y, DarkTheme.TEXT);
        y += 14;
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            int count = perCategoryFound[i].get();
            if (count == 0) continue;
            drawCenteredText(matrices, this.textRenderer, new LiteralText(CATEGORY_LABELS[i] + ": " + count),
                    this.width / 2, y, 0x77AAFF);
            y += 11;
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
