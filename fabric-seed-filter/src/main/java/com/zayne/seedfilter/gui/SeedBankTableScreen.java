package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.MenuNav;
import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.SeedBankEntry;
import com.zayne.seedfilter.SeedFilterMod;
import com.zayne.seedfilter.gui.SeedBankColumns.Column;
import com.zayne.seedfilter.mixin.AbstractButtonWidgetAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reusable sortable table for one seed bank category (BT/Village/RP) or OP sub-tab (BT+RP,
 * BT+Village, RP+Village, BT+RP+Village, Looting) - see SeedBankCategoryScreen/SeedBankOpScreen,
 * which construct one of these per tab with a different column set and stack filter.
 *
 * One row per stack (SeedBank.groupedStacks - same signature+matchedCategories+lootingLevel).
 * The header row is fixed while the body scrolls; clicking a header sorts by that column
 * (ascending first, or descending if the column says that's its natural "best first" direction -
 * see Column.defaultDescending), clicking again on the same header flips direction. Each row has
 * a Load button (Enderpearl, left-click only: joins the oldest unused seed in the stack) and a
 * Delete button (Lava Bucket: left-click clears the whole stack into history, right-click opens
 * that stack's history view instead) - replaces the old browse screen's left/right/middle-click
 * row scheme entirely.
 */
public class SeedBankTableScreen extends Screen {

    private static final int ROW_H = 20;
    private static final int HEADER_Y = 32;
    private static final int BODY_TOP = HEADER_Y + 18;
    private static final int COL_GAP = 4;
    private static final int COUNT_W = 96;

    private final Screen parent;
    private final List<Column> columns;
    private final Predicate<SeedBank.StackKey> filter;

    private SeedBank bank;
    private List<SeedBank.Stack> stacks = new ArrayList<>();
    private int sortColumnIndex = -1;
    private boolean sortDescending = false;
    private int scrollOffset = 0;

    private static final int ICON_W = 18;

    private int tableLeft, tableWidth;
    private int iconX;
    private int[] colX;
    private int countX, deleteX;

    public SeedBankTableScreen(Screen parent, String title, List<Column> columns, Predicate<SeedBank.StackKey> filter) {
        super(new LiteralText(title));
        this.parent = parent;
        this.columns = columns;
        this.filter = filter;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();

        bank = SeedBank.load(ExternalEngine.getSeedBankPath());
        stacks = new ArrayList<>();
        for (SeedBank.Stack stack : bank.groupedStacks()) {
            if ((stack.unused.isEmpty() && stack.used.isEmpty())) continue;
            if (filter.test(stack.key)) stacks.add(stack);
        }
        sortStacks();

        computeLayout();

        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height - 34, 150, 20,
                new LiteralText("Zurück"), button -> MenuNav.navigate(this.client, parent)));

        int y = BODY_TOP - scrollOffset;
        for (SeedBank.Stack stack : stacks) {
            int rowY = y;
            this.addButton(new IconClickButton(tableLeft, rowY, Items.ENDER_PEARL,
                    () -> {
                        if (!stack.unused.isEmpty()) joinFromStack(stack);
                    }, null));
            this.addButton(new IconClickButton(deleteX, rowY, Items.LAVA_BUCKET,
                    () -> {
                        if (!stack.unused.isEmpty()) {
                            bank.clearStack(stack);
                            this.init(this.client, this.width, this.height);
                        }
                    },
                    () -> MenuNav.navigate(this.client, new SeedBankHistoryScreen(this, bank, stack))));
            y += ROW_H;
        }
    }

    private void computeLayout() {
        int width = IconClickButton.SIZE + COL_GAP + ICON_W + COL_GAP;
        colX = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            width += columns.get(i).width + COL_GAP;
        }
        width += COUNT_W + COL_GAP + IconClickButton.SIZE;
        tableWidth = width;
        tableLeft = this.width / 2 - tableWidth / 2;

        iconX = tableLeft + IconClickButton.SIZE + COL_GAP;
        int x = iconX + ICON_W + COL_GAP;
        for (int i = 0; i < columns.size(); i++) {
            colX[i] = x;
            x += columns.get(i).width + COL_GAP;
        }
        countX = x;
        deleteX = countX + COUNT_W + COL_GAP;
    }

    private void sortStacks() {
        Comparator<SeedBank.Stack> cmp;
        if (sortColumnIndex < 0 || sortColumnIndex >= columns.size()) {
            cmp = Comparator.comparingInt((SeedBank.Stack s) -> s.unused.size()).reversed();
        } else {
            Column col = columns.get(sortColumnIndex);
            cmp = Comparator.comparingInt(col::sortKey);
            if (sortDescending) cmp = cmp.reversed();
        }
        stacks.sort(cmp);
    }

    private void joinFromStack(SeedBank.Stack stack) {
        SeedBankEntry entry = stack.unused.get(0);
        bank.markUsed(entry);
        MinecraftClient client = MinecraftClient.getInstance();
        SeedFilterMod.createAndJoin(client, new TitleScreen(), entry.toResult());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MenuNav.handleNavClick(this.client, button, () -> MenuNav.navigate(this.client, parent))) {
            return true;
        }
        if (button == 0 && mouseY >= HEADER_Y - 2 && mouseY <= HEADER_Y + 12) {
            for (int i = 0; i < columns.size(); i++) {
                if (mouseX >= colX[i] - COL_GAP / 2.0 && mouseX < colX[i] + columns.get(i).width + COL_GAP / 2.0) {
                    onHeaderClick(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onHeaderClick(int index) {
        if (sortColumnIndex == index) {
            sortDescending = !sortDescending;
        } else {
            sortColumnIndex = index;
            sortDescending = columns.get(index).defaultDescending;
        }
        this.init(this.client, this.width, this.height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int maxScroll = Math.max(0, stacks.size() * ROW_H - (this.height - 40 - BODY_TOP));
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (amount * ROW_H), maxScroll));
        this.init(this.client, this.width, this.height);
        return true;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 16, 0xFFFFFF);

        int visibleTop = BODY_TOP;
        int visibleBottom = this.height - 40;
        for (AbstractButtonWidget widget : this.buttons) {
            if (widget instanceof IconClickButton) {
                AbstractButtonWidgetAccessor accessor = (AbstractButtonWidgetAccessor) widget;
                int y = accessor.getY();
                accessor.setVisible(y >= visibleTop - ROW_H && y <= visibleBottom);
            }
        }

        // Sticky header - drawn every frame at a fixed Y, independent of scrollOffset.
        for (int i = 0; i < columns.size(); i++) {
            Column col = columns.get(i);
            String label = col.header;
            if (sortColumnIndex == i) {
                label += sortDescending ? " ▼" : " ▲";
            }
            drawStringWithShadow(matrices, this.textRenderer, label, colX[i], HEADER_Y, DarkTheme.HEADING);
        }
        fill(matrices, tableLeft, HEADER_Y + 12, tableLeft + tableWidth, HEADER_Y + 13, DarkTheme.BORDER);

        if (stacks.isEmpty()) {
            drawCenteredText(matrices, this.textRenderer, new LiteralText("Keine Seeds in dieser Kategorie."),
                    this.width / 2, this.height / 2, DarkTheme.TEXT_DIM);
        }

        MinecraftClient client = MinecraftClient.getInstance();
        int y = BODY_TOP - scrollOffset;
        for (SeedBank.Stack stack : stacks) {
            if (y >= visibleTop - ROW_H && y <= visibleBottom) {
                for (int i = 0; i < columns.size(); i++) {
                    drawStringWithShadow(matrices, this.textRenderer, columns.get(i).text(stack), colX[i], y + 5, DarkTheme.TEXT);
                }
                String countText = stack.unused.size() + "x" + (stack.used.isEmpty() ? "" : " (" + stack.used.size() + ")");
                int countColor = stack.unused.isEmpty() ? DarkTheme.TEXT_DIM : 0x77AAFF;
                drawStringWithShadow(matrices, this.textRenderer, countText, countX, y + 5, countColor);
                client.getItemRenderer().renderGuiItemIcon(new ItemStack(CategoryStyle.iconFor(stack.key.matchedCategories)),
                        iconX, y + 1);
            }
            y += ROW_H;
        }

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
