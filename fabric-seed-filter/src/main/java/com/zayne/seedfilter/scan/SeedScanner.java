package com.zayne.seedfilter.scan;

import com.zayne.seedfilter.FilterConfig;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.feature.StructureFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SeedScanner {

    private static final Logger LOGGER = LogManager.getLogger("seed-filter");

    public static final int WORKER_THREADS = 6;

    public static class Result {
        public final long seed;

        public Result(long seed) {
            this.seed = seed;
        }
    }

    /**
     * Scans random seeds across multiple background threads (pure math + headless biome
     * sampling, no game world involved) and completes as soon as one matches.
     * Checks: Village, Ruined Portal, Buried Treasure (position + biome).
     * NOT checked yet: Bastion, Fortress (need a Nether biome source), chest loot contents.
     */
    public static CompletableFuture<Result> scanAsync(FilterConfig config, AtomicInteger attemptsCounter, AtomicBoolean cancelled) {
        ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);
        CompletableFuture<Result> future = new CompletableFuture<>();

        for (int i = 0; i < WORKER_THREADS; i++) {
            executor.submit(() -> {
                Random rnd = new Random();
                while (!cancelled.get() && !future.isDone()) {
                    long seed = rnd.nextLong();
                    attemptsCounter.incrementAndGet();
                    if (matches(seed, config)) {
                        future.complete(new Result(seed));
                        return;
                    }
                }
            });
        }

        future.whenComplete((result, error) -> executor.shutdownNow());
        return future;
    }

    private static boolean matches(long seed, FilterConfig config) {
        VanillaLayeredBiomeSource overworldBiomes = HeadlessBiomeSource.create(seed);
        int[] spawn = SpawnFinder.findApproxOverworldSpawn(overworldBiomes);
        int spawnChunkX = spawn[0] >> 4;
        int spawnChunkZ = spawn[1] >> 4;

        if (config.villageEnabled && !checkGridStructure(seed, overworldBiomes, StructureConfig.VILLAGE,
                StructureFeatures.VILLAGE, spawnChunkX, spawnChunkZ, config.villageMaxChunks)) {
            return false;
        }

        if (config.ruinedPortalEnabled && !checkGridStructure(seed, overworldBiomes, StructureConfig.RUINED_PORTAL,
                StructureFeatures.RUINED_PORTAL, spawnChunkX, spawnChunkZ, config.ruinedPortalMaxChunks)) {
            return false;
        }

        if (config.buriedTreasureEnabled && !checkTreasure(seed, overworldBiomes, spawnChunkX, spawnChunkZ, config.buriedTreasureMaxChunks)) {
            return false;
        }

        return true;
    }

    private static boolean checkGridStructure(long seed, VanillaLayeredBiomeSource biomes, StructureConfig placement,
                                               StructureFeature<?> feature, int spawnChunkX, int spawnChunkZ, int maxChunks) {
        StructurePlacement.Candidate candidate = StructurePlacement.nearestGridCandidate(seed, placement, spawnChunkX, spawnChunkZ);
        if (candidate == null || candidate.chunkDistance > maxChunks) {
            return false;
        }
        return isValidBiome(biomes, feature, candidate.chunkX, candidate.chunkZ);
    }

    private static boolean checkTreasure(long seed, VanillaLayeredBiomeSource biomes, int spawnChunkX, int spawnChunkZ, int maxChunks) {
        StructurePlacement.Candidate candidate = StructurePlacement.nearestTreasureChunk(seed, spawnChunkX, spawnChunkZ, maxChunks);
        if (candidate == null) {
            return false;
        }
        return isValidBiome(biomes, StructureFeatures.BURIED_TREASURE, candidate.chunkX, candidate.chunkZ);
    }

    private static boolean isValidBiome(VanillaLayeredBiomeSource biomes, StructureFeature<?> feature, int chunkX, int chunkZ) {
        if (feature == null) {
            return false;
        }
        int blockX = chunkX * 16 + 8;
        int blockZ = chunkZ * 16 + 8;
        Biome biome = HeadlessBiomeSource.biomeAt(biomes, blockX, blockZ);
        return biome.hasStructureFeature(feature);
    }
}
