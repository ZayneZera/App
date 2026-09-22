package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.SeedBankEntry;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * One column of a {@link SeedBankTableScreen} - a header label, a pixel width, a way to render a
 * stack's value as text, and a way to derive a sort key from it (smaller sorts first ascending;
 * see {@link #defaultDescending} for columns like Looting level where "first" should mean
 * highest instead). The static factories below build the column kinds every category table needs
 * out of {@link SeedBankEntry.SearchSettings} (the search config that produced the stack).
 */
public final class SeedBankColumns {

    public static final class Column {
        public final String header;
        public final int width;
        public final boolean defaultDescending;
        private final Function<SeedBank.Stack, String> textFn;
        private final ToIntFunction<SeedBank.Stack> sortFn;

        public Column(String header, int width, boolean defaultDescending,
                      Function<SeedBank.Stack, String> textFn, ToIntFunction<SeedBank.Stack> sortFn) {
            this.header = header;
            this.width = width;
            this.defaultDescending = defaultDescending;
            this.textFn = textFn;
            this.sortFn = sortFn;
        }

        public String text(SeedBank.Stack stack) {
            return textFn.apply(stack);
        }

        public int sortKey(SeedBank.Stack stack) {
            return sortFn.applyAsInt(stack);
        }
    }

    /** A chunk-radius column ("6C" / "-" if that category was disabled for this search) - used
     * for Spawn/Fortress/Bastion/RP/Village style columns. Sorts shortest-first by default;
     * disabled (-1) sorts last regardless of direction (via Integer.MAX_VALUE). */
    public static Column chunks(String header, ToIntFunction<SeedBankEntry.SearchSettings> chunksFn) {
        return new Column(header, 40, false,
                stack -> {
                    int c = chunksFn.applyAsInt(stack.settings());
                    return c < 0 ? "-" : c + "C";
                },
                stack -> {
                    int c = chunksFn.applyAsInt(stack.settings());
                    return c < 0 ? Integer.MAX_VALUE : c;
                });
    }

    /** An on/off filter column (Diamond/Iron/Looting-required/...). */
    public static Column onOff(String header, Predicate<SeedBankEntry.SearchSettings> fn) {
        return new Column(header, 26, false,
                stack -> fn.test(stack.settings()) ? "§a✓" : "§7-",
                stack -> fn.test(stack.settings()) ? 0 : 1);
    }

    /** TNT's own 0/1/2+ bucketing (the search's minimum-required count, 0-2). */
    public static Column tnt() {
        return new Column("TNT", 32, false,
                stack -> {
                    int t = stack.settings().buriedTreasureMinTnt;
                    return t >= 2 ? "2+" : String.valueOf(t);
                },
                stack -> stack.settings().buriedTreasureMinTnt);
    }

    public static Column fish() {
        return new Column("Fisch", 34, false,
                stack -> String.valueOf(stack.settings().buriedTreasureMinFish),
                stack -> stack.settings().buriedTreasureMinFish);
    }

    /** Which bastion types were allowed (B=Bridge, H=Housing, S=Stables, T=Treasure). Not
     * meaningfully orderable, so its sort key just keeps stacks stable relative to each other. */
    public static Column bastionTypes() {
        return new Column("Typ", 46, false,
                stack -> {
                    SeedBankEntry.SearchSettings s = stack.settings();
                    if (s.bastionChunks < 0) return "-";
                    StringBuilder sb = new StringBuilder();
                    if (s.bastionAllowBridge) sb.append("B");
                    if (s.bastionAllowHousing) sb.append("H");
                    if (s.bastionAllowStables) sb.append("S");
                    if (s.bastionAllowTreasure) sb.append("T");
                    return sb.length() > 0 ? sb.toString() : "-";
                },
                stack -> 0);
    }

    /** The Looting side channel's sword level (2 or 3) - part of the stack key itself (see
     * SeedBank.StackKey), so it's constant across a whole stack. Defaults to highest-first. */
    public static Column lootingLevel() {
        return new Column("Stufe", 40, true,
                stack -> "Lvl " + stack.key.lootingLevel,
                stack -> stack.key.lootingLevel);
    }

    /** Whether this stack's matchedCategories bitmask includes the given bit - for the informational
     * Village/BuriedTreasure "also matched" columns on OP/Looting tables (see ExternalEngine's
     * CATEGORY_* constants and the doc on why those two are never mandatory there). */
    public static Column matchedBit(String header, int bit) {
        return new Column(header, 30, false,
                stack -> (stack.key.matchedCategories & bit) != 0 ? "§a✓" : "§7-",
                stack -> (stack.key.matchedCategories & bit) != 0 ? 0 : 1);
    }

    private SeedBankColumns() {
    }
}
