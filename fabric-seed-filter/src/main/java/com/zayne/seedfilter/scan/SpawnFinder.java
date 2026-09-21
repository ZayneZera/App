package com.zayne.seedfilter.scan;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;

/**
 * Approximate overworld spawn point finder. Real vanilla spawn selection uses a specific
 * reservoir-sampling search over a biome grid; this is a close first-match approximation
 * (same search area, first valid spawn biome found) meant to be calibrated against real
 * in-game spawn points once we can verify results.
 */
public class SpawnFinder {

    public static int[] findApproxOverworldSpawn(VanillaLayeredBiomeSource biomeSource) {
        int maxRadiusBlocks = 256;
        for (int radius = 0; radius <= maxRadiusBlocks; radius += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                for (int dx = -radius; dx <= radius; dx += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    Biome biome = HeadlessBiomeSource.biomeAt(biomeSource, dx, dz);
                    if (isValidSpawnBiome(biome)) {
                        return new int[]{dx, dz};
                    }
                }
            }
        }
        return new int[]{8, 8};
    }

    private static boolean isValidSpawnBiome(Biome biome) {
        Biome.Category category = biome.getCategory();
        return category == Biome.Category.FOREST
                || category == Biome.Category.PLAINS
                || category == Biome.Category.TAIGA
                || category == Biome.Category.JUNGLE
                || category == Biome.Category.EXTREME_HILLS;
    }
}
