package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import com.zayne.seedfilter.SeedBank;
import com.zayne.seedfilter.gui.SeedBankColumns.Column;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Routing (which tab a stack belongs in) and column-set definitions for the seed bank's tabbed
 * UI - see SeedBankCategoryScreen (top-level BT/Village/RP/OP) and SeedBankOpScreen (OP's five
 * sub-tabs). Bastion/Fortress never appear as tabs of their own since they're always-mandatory
 * side filters (see engine.c) rather than something a seed can "belong to" - every tab's table
 * still shows their chunk radius as ordinary columns.
 */
public final class SeedBankTabs {

    private static final int VILLAGE = ExternalEngine.CATEGORY_VILLAGE;
    private static final int RUINED_PORTAL = ExternalEngine.CATEGORY_RUINED_PORTAL;
    private static final int TREASURE = ExternalEngine.CATEGORY_TREASURE;
    private static final int LOOTING = ExternalEngine.CATEGORY_LOOTING_RP;

    public static boolean isLooting(SeedBank.StackKey k) {
        return (k.matchedCategories & LOOTING) != 0;
    }

    private static int flexBits(SeedBank.StackKey k) {
        return k.matchedCategories & (VILLAGE | RUINED_PORTAL | TREASURE);
    }

    /** True when this stack belongs under the top-level OP tab: a genuine bonus multi-match
     * (2-3 of Village/RuinedPortal/BuriedTreasure at once), a Looting find, or the rare edge case
     * of a match with none of the three flex categories enabled at all (only possible if the user
     * disabled Village/RuinedPortal/BuriedTreasure entirely and searched on Bastion/Fortress
     * alone) - that last case doesn't fit any of BT/Village/RP either, so OP is the safe catch-all
     * for it rather than silently hiding those entries from every tab. */
    public static boolean isOpTab(SeedBank.StackKey k) {
        if (isLooting(k)) return true;
        int count = Integer.bitCount(flexBits(k));
        return count >= 2 || count == 0;
    }

    public static boolean isVillageTab(SeedBank.StackKey k) {
        return !isOpTab(k) && flexBits(k) == VILLAGE;
    }

    public static boolean isRuinedPortalTab(SeedBank.StackKey k) {
        return !isOpTab(k) && flexBits(k) == RUINED_PORTAL;
    }

    public static boolean isBuriedTreasureTab(SeedBank.StackKey k) {
        return !isOpTab(k) && flexBits(k) == TREASURE;
    }

    public static boolean isBtRp(SeedBank.StackKey k) {
        return !isLooting(k) && flexBits(k) == (TREASURE | RUINED_PORTAL);
    }

    public static boolean isBtVillage(SeedBank.StackKey k) {
        return !isLooting(k) && flexBits(k) == (TREASURE | VILLAGE);
    }

    public static boolean isRpVillage(SeedBank.StackKey k) {
        return !isLooting(k) && flexBits(k) == (RUINED_PORTAL | VILLAGE);
    }

    public static boolean isBtRpVillage(SeedBank.StackKey k) {
        return !isLooting(k) && flexBits(k) == (TREASURE | RUINED_PORTAL | VILLAGE);
    }

    private static Column fortress() {
        return SeedBankColumns.chunks("Fort", s -> s.fortressChunks);
    }

    private static Column bastion() {
        return SeedBankColumns.chunks("Bast", s -> s.bastionChunks);
    }

    public static List<Column> villageColumns() {
        return Arrays.asList(
                SeedBankColumns.chunks("Dorf", s -> s.villageChunks),
                fortress(), bastion(), SeedBankColumns.bastionTypes());
    }

    public static List<Column> ruinedPortalColumns() {
        return Arrays.asList(
                SeedBankColumns.chunks("RP", s -> s.ruinedPortalChunks),
                fortress(), bastion(), SeedBankColumns.bastionTypes(),
                SeedBankColumns.onOff("Loot", s -> s.ruinedPortalLootingRequired));
    }

    public static List<Column> buriedTreasureColumns() {
        return Arrays.asList(
                SeedBankColumns.chunks("BT", s -> s.buriedTreasureChunks),
                fortress(), bastion(), SeedBankColumns.bastionTypes(),
                SeedBankColumns.onOff("Dia", s -> s.buriedTreasureDiamondFilter),
                SeedBankColumns.onOff("Iron", s -> s.buriedTreasureIronFilter),
                SeedBankColumns.tnt(), SeedBankColumns.fish());
    }

    /** Shared by the OP tab's BT+RP/BT+Village/RP+Village/BT+RP+Village sub-tabs - includes each
     * combined category's own radius column plus its own extra filters, Fortress/Bastion once. */
    public static List<Column> combinedColumns(boolean bt, boolean rp, boolean village) {
        List<Column> cols = new ArrayList<>();
        if (village) cols.add(SeedBankColumns.chunks("Dorf", s -> s.villageChunks));
        if (rp) cols.add(SeedBankColumns.chunks("RP", s -> s.ruinedPortalChunks));
        if (bt) cols.add(SeedBankColumns.chunks("BT", s -> s.buriedTreasureChunks));
        cols.add(fortress());
        cols.add(bastion());
        cols.add(SeedBankColumns.bastionTypes());
        if (rp) cols.add(SeedBankColumns.onOff("Loot", s -> s.ruinedPortalLootingRequired));
        if (bt) {
            cols.add(SeedBankColumns.onOff("Dia", s -> s.buriedTreasureDiamondFilter));
            cols.add(SeedBankColumns.onOff("Iron", s -> s.buriedTreasureIronFilter));
            cols.add(SeedBankColumns.tnt());
            cols.add(SeedBankColumns.fish());
        }
        return cols;
    }

    public static List<Column> lootingColumns() {
        return Arrays.asList(
                SeedBankColumns.lootingLevel(),
                fortress(), bastion(), SeedBankColumns.bastionTypes(),
                SeedBankColumns.matchedBit("Dorf", VILLAGE),
                SeedBankColumns.matchedBit("BT", TREASURE));
    }

    private SeedBankTabs() {
    }
}
