package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * Shared icon/label/bit mapping for the five top-level categories (ExternalEngine.CATEGORY_*),
 * used by every seed-bank screen so the same item always means the same category everywhere:
 * Village=Bell, Ruined Portal=Obsidian, Buried Treasure=Chest, Bastion=Blackstone,
 * Fortress=Nether Bricks. Also the "OP" (multiple categories matched at once) icon, Netherite
 * Block.
 */
public final class CategoryStyle {
    public static final int[] BITS = {
            ExternalEngine.CATEGORY_VILLAGE, ExternalEngine.CATEGORY_RUINED_PORTAL,
            ExternalEngine.CATEGORY_TREASURE, ExternalEngine.CATEGORY_BASTION, ExternalEngine.CATEGORY_FORTRESS
    };
    public static final String[] LABELS = {"Dorf", "Ruined Portal", "Buried Treasure", "Bastion", "Fortress"};
    public static final Item[] ICONS = {
            Items.BELL, Items.OBSIDIAN, Items.CHEST, Items.BLACKSTONE, Items.NETHER_BRICKS
    };

    public static final Item OP_ICON = Items.NETHERITE_BLOCK;

    /** The first matched category's icon, or OP_ICON if this is a bonus multi-match - see
     * ExternalEngine.isOp for why Bastion/Fortress never count toward that. */
    public static Item iconFor(int matchedCategories) {
        if (ExternalEngine.isOp(matchedCategories)) {
            return OP_ICON;
        }
        for (int i = 0; i < BITS.length; i++) {
            if ((matchedCategories & BITS[i]) != 0) {
                return ICONS[i];
            }
        }
        return Items.CHEST;
    }

    public static String labelFor(int matchedCategories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < BITS.length; i++) {
            if ((matchedCategories & BITS[i]) != 0) {
                if (sb.length() > 0) sb.append(" + ");
                sb.append(LABELS[i]);
            }
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }

    private CategoryStyle() {
    }
}
