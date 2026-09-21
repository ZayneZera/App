package com.zayne.seedfilter.scan;

import com.zayne.seedfilter.FilterConfig;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SeedScanner {

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
     * NOTE: only checks the Village criterion so far - the other criteria are not implemented yet.
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

        if (config.villageEnabled) {
            StructurePlacement.Candidate village = StructurePlacement.nearestGridCandidate(
                    seed, StructureConfig.VILLAGE, spawnChunkX, spawnChunkZ);
            if (village == null || village.chunkDistance > config.villageMaxChunks) {
                return false;
            }
        }

        return true;
    }
}
