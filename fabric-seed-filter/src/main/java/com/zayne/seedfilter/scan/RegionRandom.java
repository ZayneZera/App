package com.zayne.seedfilter.scan;

import java.util.Random;

/**
 * Minecraft's region-seed formula, verified against Cubitect/cubiomes (finders.c getRegPos):
 * regionSeed = regionX*341873128712 + regionZ*132897987541 + worldSeed + salt
 * Minecraft's legacy structure RNG is java.util.Random-compatible.
 */
public class RegionRandom {
    public static Random forRegion(long worldSeed, int regionX, int regionZ, long salt) {
        long regionSeed = (long) regionX * 341873128712L + (long) regionZ * 132897987541L + worldSeed + salt;
        return new Random(regionSeed);
    }
}
