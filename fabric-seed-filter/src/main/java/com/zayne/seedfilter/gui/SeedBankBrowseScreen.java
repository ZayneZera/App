package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.SeedBankEntry;
import com.zayne.seedfilter.SeedFilterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lists every saved seed bank stack (see SeedBank.groupedStacks) as one row per (settings,
 * matched-categories) combination. Left-click a row: joins the oldest unused seed in that stack
 * and marks it used. Right-click a row: switches to that stack's history (its used seeds) instead
 * of joining anything - a pure view, matching how the user asked for this ("nicht linksklicke
 * sondern rechtsklicke... komme ich auf die Historie").
 */
public class SeedBankBrowseScreen extends Screen {

    private final Screen parent;
    private final SeedBank bank;
    private List<SeedBank.Stack> stacks = new ArrayList<>();

    /** Non-null while showing one stack's used-seed history instead of the main stack list. */
    private SeedBank.Stack historyView = null;

    private final List<Row> rows = new ArrayList<>();
    private int scrollOffset = 0;

    private static final class Row {
        final int y;
        final SeedBank.Stack stack; // main-list row
        final SeedBankEntry historyEntry; // history-view row (stack is null in that case)

        Row(int y, SeedBank.Stack stack) {
            this.y = y;
            this.stack = stack;
            this.historyEntry = null;
        }

        Row(int y, SeedBankEntry historyEntry) {
            this.y = y;
            this.stack = null;
            this.historyEntry = historyEntry;
        }
    }

    public SeedBankBrowseScreen(Screen parent) {
        super(new LiteralText("Seedbank"));
        this.parent = parent;
        this.bank = SeedBank.load(ExternalEngine.getSeedBankPath());
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        rows.clear();

        int backY = this.height - 34;
        this.addButton(new ButtonWidget(this.width / 2 - 75, backY, 150, 20,
                new LiteralText(historyView != null ? "Zurück" : "Schließen"), button -> {
            if (historyView != null) {
                historyView = null;
                this.init(this.client, this.width, this.height);
            } else {
                this.client.openScreen(this.parent);
            }
        }));

        int y = 40 - scrollOffset;
        int rowH = 16;

        if (historyView != null) {
            for (SeedBankEntry entry : historyView.used) {
                rows.add(new Row(y, entry));
                y += rowH;
            }
        } else {
            stacks = bank.groupedStacks();
            stacks.sort((a, b) -> {
                if (a.key.isOp() != b.key.isOp()) return a.key.isOp() ? -1 : 1;
                return Integer.compare(b.unused.size(), a.unused.size());
            });
            for (SeedBank.Stack stack : stacks) {
                if (stack.unused.isEmpty() && stack.used.isEmpty()) continue;
                rows.add(new Row(y, stack));
                y += rowH;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            for (Row row : rows) {
                if (mouseY < row.y || mouseY >= row.y + 16 || mouseX < this.width / 2 - 160 || mouseX > this.width / 2 + 160) {
                    continue;
                }
                if (row.stack != null) {
                    if (button == 1) {
                        historyView = row.stack;
                        this.init(this.client, this.width, this.height);
                    } else if (!row.stack.unused.isEmpty()) {
                        joinFromStack(row.stack);
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void joinFromStack(SeedBank.Stack stack) {
        SeedBankEntry entry = stack.unused.get(0);
        bank.markUsed(entry);
        MinecraftClient client = MinecraftClient.getInstance();
        SeedFilterMod.createAndJoin(client, new TitleScreen(), entry.toResult());
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        String heading = historyView != null
                ? "Historie: " + CategoryStyle.labelFor(historyView.key.matchedCategories)
                : "Seedbank";
        drawCenteredText(matrices, this.textRenderer, new LiteralText(heading), this.width / 2, 16, 0xFFFFFF);

        if (rows.isEmpty()) {
            drawCenteredText(matrices, this.textRenderer,
                    new LiteralText(historyView != null ? "Noch keine benutzten Seeds." : "Seedbank ist leer."),
                    this.width / 2, this.height / 2, DarkTheme.TEXT_DIM);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM. HH:mm");
        for (Row row : rows) {
            if (row.y < 30 || row.y > this.height - 40) continue;
            if (row.stack != null) {
                SeedBank.Stack stack = row.stack;
                String opTag = stack.key.isOp() ? "§6[OP] §r" : "";
                String text = opTag + CategoryStyle.labelFor(stack.key.matchedCategories) + "  —  " + stack.unused.size() + "x verfügbar"
                        + (stack.used.isEmpty() ? "" : " (" + stack.used.size() + " benutzt)");
                int color = stack.unused.isEmpty() ? DarkTheme.TEXT_DIM : 0x77AAFF;
                int textW = this.textRenderer.getWidth(text);
                int rowX = this.width / 2 - (textW + 18) / 2;
                client.getItemRenderer().renderGuiItemIcon(new ItemStack(CategoryStyle.iconFor(stack.key.matchedCategories)), rowX, row.y);
                drawStringWithShadow(matrices, this.textRenderer, text, rowX + 18, row.y + 4, color);
            } else if (row.historyEntry != null) {
                String text = "Seed " + row.historyEntry.seed + "  —  " + fmt.format(new Date(row.historyEntry.timestampMillis));
                drawCenteredText(matrices, this.textRenderer, new LiteralText(text), this.width / 2, row.y + 4, DarkTheme.TEXT_DIM);
            }
        }

        if (historyView == null && !rows.isEmpty()) {
            drawCenteredText(matrices, this.textRenderer,
                    new LiteralText("Linksklick: laden  |  Rechtsklick: Historie"),
                    this.width / 2, this.height - 52, DarkTheme.TEXT_DIM);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, rows.size() * 16 - (this.height - 80));
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (amount * 16), maxScroll));
        this.init(this.client, this.width, this.height);
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
