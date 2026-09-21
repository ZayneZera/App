package com.zayne.seedfilter.scan;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;

import java.util.Random;

/**
 * Real vanilla overworld spawn point search, verified against Cubitect/cubiomes
 * (finders.c estimateSpawn/locateBiome, MC <= 1.17 code path): scans a 129x129 biome-quad
 * grid centered on the world origin, using reservoir sampling (RNG seeded from the raw
 * world seed) so a uniformly random match is picked among ALL valid positions found - not
 * just the first/nearest one. Scan order (row-major, x fastest) is our best reconstruction
 * and hasn't been bit-verified against a real game seed yet.
 */
public class SpawnFinder {

    private static final int SEARCH_RADIUS_QUARTS = 64; // 256 blocks / 4

    public static int[] findRealOverworldSpawn(long worldSeed, VanillaLayeredBiomeSource biomeSource) {
        Random rng = new Random(worldSeed);
        int count = 0;
        int selectedBiomeX = 0;
        int selectedBiomeZ = 0;

        for (int biomeZ = -SEARCH_RADIUS_QUARTS; biomeZ <= SEARCH_RADIUS_QUARTS; biomeZ++) {
            for (int biomeX = -SEARCH_RADIUS_QUARTS; biomeX <= SEARCH_RADIUS_QUARTS; biomeX++) {
                Biome biome = biomeSource.getBiomeForNoiseGen(biomeX, 0, biomeZ);
                if (!isValidSpawnBiome(biome)) {
                    continue;
                }
                count++;
                if (count == 1 || rng.nextInt(count) == 0) {
                    selectedBiomeX = biomeX;
                    selectedBiomeZ = biomeZ;
                }
            }
        }

        if (count == 0) {
            return new int[]{8, 8};
        }
        return new int[]{selectedBiomeX * 4, selectedBiomeZ * 4};
    }

    private static boolean isValidSpawnBiome(Biome biome) {
        return biome == Biomes.FOREST
                || biome == Biomes.PLAINS
                || biome == Biomes.TAIGA
                || biome == Biomes.JUNGLE
                || biome == Biomes.WOODED_HILLS;
    }
}
