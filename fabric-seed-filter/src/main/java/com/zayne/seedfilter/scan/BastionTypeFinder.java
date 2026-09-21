package com.zayne.seedfilter.scan;

import java.util.Random;

/**
 * Determines which of the 4 Bastion Remnant sub-structures spawns at a given structure-start
 * chunk, verified against Cubitect/cubiomes (finders.c getVariant(), Bastion case).
 *
 * RNG: seed a Random with the world seed, draw two nextLong() values, combine them with the
 * structure-start chunk coords and the raw world seed (matches cubiomes' chunkGenerateRnd,
 * which is itself the same jigsaw-piece RNG vanilla uses for bastion/fortress/village piece
 * selection), then re-seed a fresh Random with that combined value. The first nextInt(4) draw
 * from that Random is the type index - but ONLY for MC 1.16.1: cubiomes explicitly documents
 * that 1.16.1 has rotation/start swapped compared to every other version, so what's normally
 * the *second* draw (rotation) is 1.16.1's type index. We target 1.16.1 exclusively, so we
 * use the first draw.
 */
public class BastionTypeFinder {

    public enum Type { HOUSING, STABLES, TREASURE, BRIDGE }

    private static final Type[] TYPES_BY_INDEX = { Type.HOUSING, Type.STABLES, Type.TREASURE, Type.BRIDGE };

    public static Type findType(long worldSeed, int structureChunkX, int structureChunkZ) {
        Random seedRandom = new Random(worldSeed);
        long a = seedRandom.nextLong();
        long b = seedRandom.nextLong();
        long combined = (a * structureChunkX) ^ (b * structureChunkZ) ^ worldSeed;

        Random pieceRandom = new Random(combined);
        int typeIndex = pieceRandom.nextInt(4);
        return TYPES_BY_INDEX[typeIndex];
    }

    public static boolean isAllowed(Type type, boolean allowHousing, boolean allowStables,
                                     boolean allowTreasure, boolean allowBridge) {
        switch (type) {
            case HOUSING: return allowHousing;
            case STABLES: return allowStables;
            case TREASURE: return allowTreasure;
            case BRIDGE: return allowBridge;
            default: return false;
        }
    }
}
