package com.zayne.seedfilter.scan;

import com.zayne.seedfilter.FilterConfig;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
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
        public final int spawnX;
        public final int spawnZ;

        public Result(long seed, int spawnX, int spawnZ) {
            this.seed = seed;
            this.spawnX = spawnX;
            this.spawnZ = spawnZ;
        }
    }

    /**
     * Scans random seeds across multiple background threads (pure math + headless biome
     * sampling, no game world involved) and completes as soon as one matches.
     * Checks: Village, Ruined Portal, Buried Treasure, Bastion (position + biome + type), Fortress.
     * NOT checked yet: chest loot contents.
     */
    public static CompletableFuture<Result> scanAsync(FilterConfig config, AtomicInteger attemptsCounter, AtomicBoolean cancelled) {
        return scanAsync(config, attemptsCounter, cancelled, null);
    }

    public static CompletableFuture<Result> scanAsync(FilterConfig config, AtomicInteger attemptsCounter, AtomicBoolean cancelled, ScanStats stats) {
        ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);
        CompletableFuture<Result> future = new CompletableFuture<>();

        for (int i = 0; i < WORKER_THREADS; i++) {
            executor.submit(() -> {
                Random rnd = new Random();
                while (!cancelled.get() && !future.isDone()) {
                    long seed = rnd.nextLong();
                    attemptsCounter.incrementAndGet();
                    int[] spawn = matches(seed, config, stats);
                    if (spawn != null) {
                        // matches() already re-verified against the exact spawn before
                        // returning non-null, so `spawn` here IS the exact one.
                        future.complete(new Result(seed, spawn[0], spawn[1]));
                        return;
                    }
                }
            });
        }

        future.whenComplete((result, error) -> executor.shutdownNow());
        return future;
    }

    /**
     * Returns the computed exact [spawnX, spawnZ] on a match, or null if the seed doesn't
     * qualify. stats may be null (no funnel tracking needed).
     *
     * Two-pass: the exact spawn search (findRealOverworldSpawn) scans a 129x129 biome grid -
     * expensive, and pointless to pay for on the ~98%+ of candidates that fail Buried Treasure
     * or another cheap criterion anyway. So we first sieve using a cheap approximate spawn
     * (findApproxOverworldSpawn, stops at the first valid biome). Only a seed that ALREADY
     * passes every criterion against the approximation gets the expensive exact spawn search
     * and a full re-check against it - since approx and exact can differ enough to flip a tight
     * distance check, the final accept/reject decision always comes from the exact pass, never
     * the sieve. Funnel stats are only recorded on that final, accurate pass.
     */
    private static int[] matches(long seed, FilterConfig config, ScanStats stats) {
        VanillaLayeredBiomeSource overworldBiomes = HeadlessBiomeSource.create(seed);

        int[] approxSpawn = SpawnFinder.findApproxOverworldSpawn(overworldBiomes);
        if (!checkAllCriteria(seed, config, overworldBiomes, approxSpawn, null)) {
            return null;
        }

        int[] exactSpawn = SpawnFinder.findRealOverworldSpawn(seed, overworldBiomes);
        if (!checkAllCriteria(seed, config, overworldBiomes, exactSpawn, stats)) {
            return null;
        }
        return exactSpawn;
    }

    private static boolean checkAllCriteria(long seed, FilterConfig config, VanillaLayeredBiomeSource overworldBiomes,
                                             int[] spawn, ScanStats stats) {
        int spawnChunkX = spawn[0] >> 4;
        int spawnChunkZ = spawn[1] >> 4;

        if (config.villageEnabled) {
            if (stats != null) stats.reachedVillage.incrementAndGet();
            if (!checkOverworldGridStructure(seed, overworldBiomes, StructureConfig.VILLAGE,
                    StructureFeatures.VILLAGE, spawnChunkX, spawnChunkZ, config.villageMaxChunks)) {
                return false;
            }
            if (stats != null) stats.passedVillage.incrementAndGet();
        }

        if (config.ruinedPortalEnabled) {
            if (stats != null) stats.reachedRuinedPortal.incrementAndGet();
            if (!checkOverworldGridStructure(seed, overworldBiomes, StructureConfig.RUINED_PORTAL,
                    StructureFeatures.RUINED_PORTAL, spawnChunkX, spawnChunkZ, config.ruinedPortalMaxChunks)) {
                return false;
            }
            if (stats != null) stats.passedRuinedPortal.incrementAndGet();
        }

        if (config.buriedTreasureEnabled) {
            if (stats != null) stats.reachedTreasure.incrementAndGet();
            if (!checkTreasure(seed, overworldBiomes, spawnChunkX, spawnChunkZ, config.buriedTreasureMaxChunks)) {
                return false;
            }
            if (stats != null) stats.passedTreasure.incrementAndGet();
        }

        if (config.bastionEnabled || config.fortressEnabled) {
            // Nether reference point: overworld spawn divided by 8 (overworld<->nether coord ratio).
            int netherChunkX = (spawn[0] / 8) >> 4;
            int netherChunkZ = (spawn[1] / 8) >> 4;
            MultiNoiseBiomeSource netherBiomes = HeadlessNetherBiomeSource.create(seed);

            if (config.bastionEnabled) {
                if (stats != null) stats.reachedBastion.incrementAndGet();
                if (!checkBastion(seed, netherBiomes, netherChunkX, netherChunkZ, config)) {
                    return false;
                }
                if (stats != null) stats.passedBastion.incrementAndGet();
            }

            if (config.fortressEnabled) {
                if (stats != null) stats.reachedFortress.incrementAndGet();
                if (!checkFortress(seed, netherBiomes, netherChunkX, netherChunkZ, config.fortressMaxNetherChunks)) {
                    return false;
                }
                if (stats != null) stats.passedFortress.incrementAndGet();
            }
        }

        return true;
    }

    private static boolean checkOverworldGridStructure(long seed, VanillaLayeredBiomeSource biomes, StructureConfig placement,
                                                         StructureFeature<?> feature, int spawnChunkX, int spawnChunkZ, int maxChunks) {
        StructurePlacement.Candidate candidate = StructurePlacement.nearestGridCandidate(seed, placement, spawnChunkX, spawnChunkZ);
        if (candidate == null || candidate.chunkDistance > maxChunks) {
            return false;
        }
        int blockX = candidate.chunkX * 16 + 8;
        int blockZ = candidate.chunkZ * 16 + 8;
        Biome biome = HeadlessBiomeSource.biomeAt(biomes, blockX, blockZ);
        return feature != null && biome.hasStructureFeature(feature);
    }

    private static boolean checkBastion(long seed, MultiNoiseBiomeSource biomes, int centerChunkX, int centerChunkZ, FilterConfig config) {
        NetherStructurePlacement.Hit hit = NetherStructurePlacement.findNearest(
                seed, NetherStructurePlacement.Type.BASTION, centerChunkX, centerChunkZ, biomes);
        if (hit == null || hit.chunkDistance > config.bastionMaxNetherChunks) {
            return false;
        }
        BastionTypeFinder.Type type = BastionTypeFinder.findType(seed, hit.chunkX, hit.chunkZ);
        return BastionTypeFinder.isAllowed(type, config.bastionAllowHousing, config.bastionAllowStables,
                config.bastionAllowTreasure, config.bastionAllowBridge);
    }

    private static boolean checkFortress(long seed, MultiNoiseBiomeSource biomes, int centerChunkX, int centerChunkZ, int maxChunks) {
        NetherStructurePlacement.Hit hit = NetherStructurePlacement.findNearest(
                seed, NetherStructurePlacement.Type.FORTRESS, centerChunkX, centerChunkZ, biomes);
        return hit != null && hit.chunkDistance <= maxChunks;
    }

    private static boolean checkTreasure(long seed, VanillaLayeredBiomeSource biomes, int spawnChunkX, int spawnChunkZ, int maxChunks) {
        StructurePlacement.Candidate candidate = StructurePlacement.nearestTreasureChunk(seed, spawnChunkX, spawnChunkZ, maxChunks);
        if (candidate == null) {
            return false;
        }
        int blockX = candidate.chunkX * 16 + 8;
        int blockZ = candidate.chunkZ * 16 + 8;
        Biome biome = HeadlessBiomeSource.biomeAt(biomes, blockX, blockZ);
        return biome.hasStructureFeature(StructureFeatures.BURIED_TREASURE);
    }
}
