package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.MenuNav;
import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.SeedBankEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * List of one stack's already-used ("joined") seeds, newest first - reached by right-clicking a
 * row's Delete button in {@link SeedBankTableScreen}. Otherwise view-only except for the one
 * "delete all" action, which permanently removes every entry shown here (unlike the table's own
 * Delete button, this doesn't move anything - there's nowhere further for a history entry to go).
 */
public class SeedBankHistoryScreen extends Screen {

    private final Screen parent;
    private final SeedBank bank;
    private final SeedBank.Stack stack;
    private int scrollOffset = 0;

    public SeedBankHistoryScreen(Screen parent, SeedBank bank, SeedBank.Stack stack) {
        super(new LiteralText("Historie: " + CategoryStyle.labelFor(stack.key.matchedCategories)));
        this.parent = parent;
        this.bank = bank;
        this.stack = stack;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height - 34, 150, 20,
                new LiteralText("Zurück"), button -> MenuNav.navigate(this.client, parent)));
        this.addButton(new IconClickButton(this.width / 2 + 90, this.height - 34, Items.LAVA_BUCKET,
                () -> {
                    if (!stack.used.isEmpty()) {
                        bank.deleteHistory(stack);
                        MenuNav.navigate(this.client, parent);
                    }
                }, null));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MenuNav.handleNavClick(this.client, button, () -> MenuNav.navigate(this.client, parent))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, stack.used.size() * 16 - (this.height - 80));
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (amount * 16), maxScroll));
        return true;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);

        if (stack.used.isEmpty()) {
            drawCenteredText(matrices, this.textRenderer, new LiteralText("Noch keine benutzten Seeds."),
                    this.width / 2, this.height / 2, DarkTheme.TEXT_DIM);
        }

        SimpleDateFormat fmt = new SimpleDateFormat("dd.MM. HH:mm");
        int y = 40 - scrollOffset;
        for (SeedBankEntry entry : stack.used) {
            if (y >= 30 && y <= this.height - 40) {
                String text = "Seed " + entry.seed + "  —  " + fmt.format(new Date(entry.timestampMillis));
                drawCenteredText(matrices, this.textRenderer, new LiteralText(text), this.width / 2, y + 4, DarkTheme.TEXT_DIM);
            }
            y += 16;
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
