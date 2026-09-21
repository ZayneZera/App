package com.zayne.seedfilter.scan;

import java.util.Random;

public class StructurePlacement {

    public static class Candidate {
        public final int chunkX;
        public final int chunkZ;
        public final int chunkDistance;

        Candidate(int chunkX, int chunkZ, int chunkDistance) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunkDistance = chunkDistance;
        }
    }

    /** Region-grid structures (Village, Ruined Portal, Bastion, Fortress). */
    public static Candidate nearestGridCandidate(long worldSeed, StructureConfig config, int centerChunkX, int centerChunkZ) {
        int spacing = config.spacing;
        int centerRegionX = Math.floorDiv(centerChunkX, spacing);
        int centerRegionZ = Math.floorDiv(centerChunkZ, spacing);

        Candidate best = null;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int regionX = centerRegionX + dx;
                int regionZ = centerRegionZ + dz;

                Random random = RegionRandom.forRegion(worldSeed, regionX, regionZ, config.salt);
                int offsetX = random.nextInt(config.separation);
                int offsetZ = random.nextInt(config.separation);

                int chunkX = regionX * spacing + offsetX;
                int chunkZ = regionZ * spacing + offsetZ;

                int dist = Math.max(Math.abs(chunkX - centerChunkX), Math.abs(chunkZ - centerChunkZ));
                if (best == null || dist < best.chunkDistance) {
                    best = new Candidate(chunkX, chunkZ, dist);
                }
            }
        }
        return best;
    }

    /** Buried Treasure: 1% chance per chunk (spacing/separation = 1), scanned outward from center. */
    public static Candidate nearestTreasureChunk(long worldSeed, int centerChunkX, int centerChunkZ, int maxRadiusChunks) {
        for (int r = 0; r <= maxRadiusChunks; r++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;
                    Random random = RegionRandom.forRegion(worldSeed, chunkX, chunkZ, StructureConfig.BURIED_TREASURE.salt);
                    if (random.nextFloat() < 0.01f) {
                        return new Candidate(chunkX, chunkZ, r);
                    }
                }
            }
        }
        return null;
    }
}
