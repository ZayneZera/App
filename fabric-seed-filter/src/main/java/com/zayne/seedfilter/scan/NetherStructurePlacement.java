package com.zayne.seedfilter.scan;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;

import java.util.Random;

/**
 * Bastion Remnant and Nether Fortress deliberately share the SAME 27/23 placement grid
 * (salt 30084232) so they never overlap - only one of them can ever generate per grid slot.
 * Verified against Cubitect/cubiomes (finders.c getStructurePos, Bastion/Fortress cases,
 * MC 1.16.1 branch): after the usual offsetX/offsetZ draws from the region's Random, ONE
 * MORE nextInt(5) draw from that SAME Random instance decides the winner - Fortress if the
 * result is < 2 (40%), Bastion if >= 2 (60%) - independent of biome. The biome is then
 * checked separately, only against whichever structure won the draw.
 *
 * The previous implementation skipped this selector entirely and just asked "does the biome
 * support structure X" - true for both structures in several shared nether biomes, so it
 * reported false positives for whichever structure actually lost the coin flip at the
 * nearest slot (that structure's real nearest instance is often a full region away).
 */
public class NetherStructurePlacement {

    public enum Type { BASTION, FORTRESS }

    public static class Hit {
        public final int chunkX;
        public final int chunkZ;
        public final int chunkDistance;

        Hit(int chunkX, int chunkZ, int chunkDistance) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunkDistance = chunkDistance;
        }
    }

    private static final int SPACING = 27;
    private static final int SEPARATION = 23;
    private static final long SALT = 30084232L;

    public static Hit findNearest(long worldSeed, Type wanted, int centerChunkX, int centerChunkZ, MultiNoiseBiomeSource biomes) {
        int centerRegionX = Math.floorDiv(centerChunkX, SPACING);
        int centerRegionZ = Math.floorDiv(centerChunkZ, SPACING);

        Hit best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int regionX = centerRegionX + dx;
                int regionZ = centerRegionZ + dz;

                Random random = RegionRandom.forRegion(worldSeed, regionX, regionZ, SALT);
                int offsetX = random.nextInt(SEPARATION);
                int offsetZ = random.nextInt(SEPARATION);
                // Same Random instance, continued: decides which of the two structures
                // actually claims this grid slot.
                int selector = random.nextInt(5);
                Type winner = selector < 2 ? Type.FORTRESS : Type.BASTION;
                if (winner != wanted) {
                    continue;
                }

                int chunkX = regionX * SPACING + offsetX;
                int chunkZ = regionZ * SPACING + offsetZ;

                Biome biome = HeadlessNetherBiomeSource.biomeAt(biomes, chunkX * 16 + 8, chunkZ * 16 + 8);
                boolean biomeOk = wanted == Type.BASTION
                        ? biome.hasStructureFeature(StructureFeatures.BASTION_REMNANT)
                        : biome.hasStructureFeature(StructureFeatures.FORTRESS);
                if (!biomeOk) {
                    continue;
                }

                double ddx = chunkX - centerChunkX;
                double ddz = chunkZ - centerChunkZ;
                double distSq = ddx * ddx + ddz * ddz;
                if (best == null || distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = new Hit(chunkX, chunkZ, (int) Math.round(Math.sqrt(distSq)));
                }
            }
        }
        return best;
    }
}
