package com.zayne.seedfilter;

import net.minecraft.util.math.BlockPos;

/**
 * Per-template frame geometry for the 7 Ruined Portal variants the seed-filter-engine can
 * determine placement for (see seed-filter-engine/src/frame.h for why only these 7). Mirrors
 * seed-filter-engine/src/frame.c's (now-removed) RP_CELLS_PORTAL_* tables exactly - extracted by
 * parsing the actual ruined_portal/*.nbt structure files from the 1.16.1 client jar.
 *
 * Local coordinates are relative to each template's own (0,0,0) NBT origin corner. Frame cells are
 * the border-minus-corner positions of the portal's obsidian frame; isObsidianTemplate=true means
 * the template places obsidian there (which may or may not have become Crying Obsidian at
 * generation time - that's what {@link RuinedPortalVerifier} checks for real once in-game);
 * isObsidianTemplate=false means the template places air there.
 */
public final class RuinedPortalTemplates {

    public static final class FrameCell {
        public final int x, y, z;
        public final boolean isObsidianTemplate;

        FrameCell(int x, int y, int z, boolean isObsidianTemplate) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isObsidianTemplate = isObsidianTemplate;
        }
    }

    public static final class Template {
        public final int commonIndex;
        public final int sizeX, sizeY, sizeZ;
        public final BlockPos chestLocal;
        public final FrameCell[] cells;

        Template(int commonIndex, int sizeX, int sizeY, int sizeZ, int chestX, int chestY, int chestZ, int[][] cellData) {
            this.commonIndex = commonIndex;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.chestLocal = new BlockPos(chestX, chestY, chestZ);
            this.cells = new FrameCell[cellData.length];
            for (int i = 0; i < cellData.length; i++) {
                int[] c = cellData[i];
                this.cells[i] = new FrameCell(c[0], c[1], c[2], c[3] != 0);
            }
        }
    }

    private static final Template[] TEMPLATES = new Template[] {
            new Template(0, 6, 10, 6, 2, 2, 0, new int[][] {
                    {3, 2, 2, 1}, {3, 2, 3, 1}, {3, 3, 1, 1}, {3, 3, 4, 1}, {3, 4, 1, 1},
                    {3, 4, 4, 0}, {3, 5, 1, 1}, {3, 5, 4, 0}, {3, 6, 2, 1}, {3, 6, 3, 1},
            }),
            new Template(1, 9, 12, 9, 8, 2, 6, new int[][] {
                    {5, 4, 3, 0}, {5, 4, 4, 0}, {5, 5, 2, 1}, {5, 5, 5, 0}, {5, 6, 2, 1},
                    {5, 6, 5, 0}, {5, 7, 2, 1}, {5, 7, 5, 1}, {5, 8, 3, 1}, {5, 8, 4, 1},
            }),
            new Template(2, 8, 9, 9, 3, 3, 6, new int[][] {
                    {4, 3, 3, 1}, {4, 3, 4, 1}, {4, 4, 2, 0}, {4, 4, 5, 1}, {4, 5, 2, 0},
                    {4, 5, 5, 1}, {4, 6, 2, 0}, {4, 6, 5, 1}, {4, 7, 3, 0}, {4, 7, 4, 1},
            }),
            new Template(3, 8, 9, 9, 3, 3, 2, new int[][] {
                    {4, 3, 3, 1}, {4, 3, 4, 1}, {4, 4, 2, 1}, {4, 4, 5, 1}, {4, 5, 2, 1},
                    {4, 5, 5, 1}, {4, 6, 2, 1}, {4, 6, 5, 0}, {4, 7, 3, 0}, {4, 7, 4, 0},
            }),
            new Template(5, 5, 7, 7, 1, 1, 4, new int[][] {
                    {2, 1, 1, 1}, {2, 1, 2, 1}, {2, 1, 3, 1}, {2, 2, 0, 1}, {2, 2, 4, 1},
                    {2, 3, 0, 1}, {2, 3, 4, 1}, {2, 4, 0, 1}, {2, 4, 4, 1}, {2, 5, 1, 1},
                    {2, 5, 2, 0}, {2, 5, 3, 1},
            }),
            new Template(7, 14, 9, 9, 4, 4, 2, new int[][] {
                    {5, 3, 3, 1}, {5, 3, 4, 1}, {5, 4, 2, 1}, {5, 4, 5, 0}, {5, 5, 2, 1},
                    {5, 5, 5, 0}, {5, 6, 2, 1}, {5, 6, 5, 0}, {5, 7, 3, 0}, {5, 7, 4, 0},
            }),
            new Template(8, 10, 8, 9, 4, 1, 0, new int[][] {
                    {4, 1, 4, 1}, {4, 1, 5, 1}, {4, 2, 3, 1}, {4, 2, 6, 1}, {4, 3, 3, 0},
                    {4, 3, 6, 1}, {4, 4, 3, 0}, {4, 4, 6, 1}, {4, 5, 4, 1}, {4, 5, 5, 1},
            }),
    };

    public static Template byCommonIndex(int commonIndex) {
        for (Template t : TEMPLATES) {
            if (t.commonIndex == commonIndex) return t;
        }
        return null;
    }

    /**
     * Mirrors seed-filter-engine/src/mc_random.h's mc_transformAround(): vanilla
     * Structure.transformAround(pos, mirror, rotation, pivot), Y untouched. mirror: 0=NONE,
     * 2=FRONT_BACK (Ruined Portal never rolls LEFT_RIGHT). rotation: 0=NONE, 1=CLOCKWISE_90,
     * 2=CLOCKWISE_180, 3=COUNTERCLOCKWISE_90.
     */
    public static BlockPos transformAround(int x, int y, int z, int mirror, int rotation, int pivotX, int pivotZ) {
        if (mirror == 2) {
            x = -x;
        }
        int rx, rz;
        switch (rotation) {
            case 3:
                rx = pivotX - pivotZ + z;
                rz = pivotX + pivotZ - x;
                break;
            case 1:
                rx = pivotX + pivotZ - z;
                rz = pivotZ - pivotX + x;
                break;
            case 2:
                rx = pivotX + pivotX - x;
                rz = pivotZ + pivotZ - z;
                break;
            default:
                rx = x;
                rz = z;
                break;
        }
        return new BlockPos(rx, y, rz);
    }

    private RuinedPortalTemplates() {
    }
}
