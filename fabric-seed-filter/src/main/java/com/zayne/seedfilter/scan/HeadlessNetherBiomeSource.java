package com.zayne.seedfilter.scan;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;

/**
 * Builds a real vanilla nether biome source for a given seed without a running world.
 * Nether uses the newer multi-noise biome system (not the overworld's layered/voronoi one).
 */
public class HeadlessNetherBiomeSource {

    public static MultiNoiseBiomeSource create(long seed) {
        return MultiNoiseBiomeSource.Preset.NETHER.getBiomeSource(seed);
    }

    public static Biome biomeAt(MultiNoiseBiomeSource source, int blockX, int blockZ) {
        return source.getBiomeForNoiseGen(blockX >> 2, 0, blockZ >> 2);
    }
}
