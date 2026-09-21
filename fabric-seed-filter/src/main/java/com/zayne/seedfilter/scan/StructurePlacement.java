package com.zayne.seedfilter.scan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

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
        double bestDistSq = Double.MAX_VALUE;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int regionX = centerRegionX + dx;
                int regionZ = centerRegionZ + dz;

                Random random = RegionRandom.forRegion(worldSeed, regionX, regionZ, config.salt);
                int offsetX = random.nextInt(config.separation);
                int offsetZ = random.nextInt(config.separation);

                int chunkX = regionX * spacing + offsetX;
                int chunkZ = regionZ * spacing + offsetZ;

                // True straight-line chunk distance, not chebyshev/square distance - a
                // "max N chunks" square bound allows up to N*sqrt(2) blocks on the diagonal,
                // which is what made bastions/villages appear much farther away in practice
                // than the configured limit suggested.
                double ddx = chunkX - centerChunkX;
                double ddz = chunkZ - centerChunkZ;
                double distSq = ddx * ddx + ddz * ddz;
                if (best == null || distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = new Candidate(chunkX, chunkZ, (int) Math.round(Math.sqrt(distSq)));
                }
            }
        }
        return best;
    }

    private static final Map<Integer, int[][]> TREASURE_OFFSET_CACHE = new ConcurrentHashMap<>();

    /** Offsets within radius r (true circle, not a square), sorted nearest-first. Cached per r. */
    private static int[][] treasureOffsetsSortedByDistance(int maxRadiusChunks) {
        return TREASURE_OFFSET_CACHE.computeIfAbsent(maxRadiusChunks, r -> {
            List<int[]> offsets = new ArrayList<>();
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (dx * dx + dz * dz <= r * r) {
                        offsets.add(new int[]{dx, dz});
                    }
                }
            }
            offsets.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
            return offsets.toArray(new int[0][]);
        });
    }

    /**
     * Buried Treasure: 1% chance per chunk (spacing/separation = 1), scanned outward from
     * center in true (circular) distance order - not a Chebyshev/square scan, which let
     * matches slip in up to sqrt(2)x farther away (diagonally) than the configured limit.
     */
    public static Candidate nearestTreasureChunk(long worldSeed, int centerChunkX, int centerChunkZ, int maxRadiusChunks) {
        for (int[] offset : treasureOffsetsSortedByDistance(maxRadiusChunks)) {
            int chunkX = centerChunkX + offset[0];
            int chunkZ = centerChunkZ + offset[1];
            Random random = RegionRandom.forRegion(worldSeed, chunkX, chunkZ, StructureConfig.BURIED_TREASURE.salt);
            if (random.nextFloat() < 0.01f) {
                int dist = (int) Math.round(Math.sqrt(offset[0] * (double) offset[0] + offset[1] * (double) offset[1]));
                return new Candidate(chunkX, chunkZ, dist);
            }
        }
        return null;
    }
}
