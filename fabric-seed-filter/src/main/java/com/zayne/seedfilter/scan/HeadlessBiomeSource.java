package com.zayne.seedfilter.scan;

import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;

/**
 * Builds a real vanilla overworld biome source for a given seed without a running world,
 * so structure/spawn validation is 100% accurate (real Minecraft biome noise, not reimplemented).
 */
public class HeadlessBiomeSource {

    public static VanillaLayeredBiomeSource create(long seed) {
        return new VanillaLayeredBiomeSource(seed, false, false, Registry.BIOME);
    }

    public static Biome biomeAt(VanillaLayeredBiomeSource source, int blockX, int blockZ) {
        return source.getBiomeForNoiseGen(blockX >> 2, 0, blockZ >> 2);
    }
}
