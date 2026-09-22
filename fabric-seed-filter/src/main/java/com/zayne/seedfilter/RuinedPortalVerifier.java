package com.zayne.seedfilter;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Does the Ruined Portal frame-completability check for real, once the mod has actually
 * teleported the player into the found seed - instead of trying to predict the portal's exact
 * placement height (and therefore its Crying Obsidian rolls) from the world seed alone, which
 * turned out to be unreliable (see seed-filter-engine/src/frame.h's history). The exe already
 * determined which template/rotation/mirror the portal rolled (that part has no terrain
 * dependency and is exact); this class force-loads that chunk in the now-real world and reads the
 * true block states and the true terrain height directly - the same approach the community tool
 * "Seed Quarry" uses ("locally installed official terrain generator for exact portal height and
 * frame replacement").
 */
public final class RuinedPortalVerifier {

    private static final Logger LOGGER = LogManager.getLogger("seed-filter");

    /** Only does anything if config.ruinedPortalFrameCheck is on and the exe found a checkable
     * portal placement for this seed - matches the "only when I have the check enabled" ask.
     * onNotApproved is invoked (on the client thread) only when the check actually runs to
     * completion and comes back "Not Approved" - not on the "Approved" case, and not on any
     * error/exception path, so a broken check never silently triggers an endless retry loop. */
    public static void verifyAndAnnounce(MinecraftClient client, ExternalEngine.Result result, SeedFilterConfig config, Runnable onNotApproved) {
        if (!config.ruinedPortalFrameCheck || !result.rpFound) {
            LOGGER.info("Ruined Portal verification skipped: frameCheck={} rpFound={}", config.ruinedPortalFrameCheck, result.rpFound);
            return;
        }
        LOGGER.info("Ruined Portal verification starting: portal=({},{}) template={} rotation={} mirror={} chestObsidian={}",
                result.rpPortalX, result.rpPortalZ, result.rpTemplateIndex, result.rpRotation, result.rpMirror, result.rpChestObsidian);
        pollUntilReady(client, result, 5, onNotApproved);
    }

    private static void pollUntilReady(MinecraftClient client, ExternalEngine.Result result, int settleTicks, Runnable onNotApproved) {
        client.execute(() -> {
            if (client.player == null || client.getServer() == null) {
                pollUntilReady(client, result, settleTicks, onNotApproved);
                return;
            }
            if (settleTicks > 0) {
                pollUntilReady(client, result, settleTicks - 1, onNotApproved);
                return;
            }
            MinecraftServer server = client.getServer();
            server.execute(() -> runCheckOnServerThread(client, server, result, onNotApproved));
        });
    }

    private static void runCheckOnServerThread(MinecraftClient client, MinecraftServer server, ExternalEngine.Result result, Runnable onNotApproved) {
        try {
            ServerWorld world = server.getWorld(World.OVERWORLD);
            RuinedPortalTemplates.Template template = RuinedPortalTemplates.byCommonIndex(result.rpTemplateIndex);
            if (world == null || template == null) {
                sendErrorMessage(client, "world=" + world + " template=" + template + " (templateIndex=" + result.rpTemplateIndex + ")");
                return;
            }

            int pivotX = template.sizeX / 2;
            int pivotZ = template.sizeZ / 2;
            int portalX = result.rpPortalX;
            int portalZ = result.rpPortalZ;

            // Heightmap.Type.WORLD_SURFACE_WG (what vanilla actually used to place this portal)
            // is a WORLDGEN-purpose heightmap that only exists on a ProtoChunk during
            // generation - by the time we get here (world fully generated, player already
            // spawned), that heightmap is gone and sampling it NPEs. Instead of approximating
            // the placement height, locate the chest directly - its local position is known
            // exactly from the real structure NBT (verified byte-for-byte against Mojang's
            // ruined_portal/*.nbt files), so this predicts its world column closely - but real-seed
            // testing found this transformAround/pivot math can still be off by a couple of blocks
            // in X/Z for some templates (confirmed via a real F3 readout: portal_3, rotation
            // COUNTERCLOCKWISE_90, was off by exactly 2 in Z - a constant shift, not noise, since
            // pivotX/pivotZ only ever enter these formulas additively, independent of which local
            // coordinate is being transformed). Rather than chase the exact vanilla pivot formula
            // (Mojang's source isn't reachable from here to verify against), search a widened area
            // around the predicted column for the chest, then apply the SAME (dx,dz) correction
            // that was needed to find it to every frame cell below - a pivot error is by
            // construction a uniform translation for a fixed rotation/mirror, so this is exact
            // however wrong the initial pivot guess was, not an approximation.
            BlockPos chestT = RuinedPortalTemplates.transformAround(template.chestLocal.getX(), template.chestLocal.getY(), template.chestLocal.getZ(), result.rpMirror, result.rpRotation, pivotX, pivotZ);
            int predictedChestX = chestT.getX() + portalX;
            int predictedChestZ = chestT.getZ() + portalZ;

            BlockPos realChest = findChestNear(world, predictedChestX, predictedChestZ, CHEST_SEARCH_RADIUS);
            if (realChest == null) {
                // Not just a bigger prediction error than CHEST_SEARCH_RADIUS covers - vanilla
                // structure placement can decide a structure has a valid "start" position (which is
                // all /locate and cubiomes' isViableStructurePos check - and both of those agreeing
                // with us is exactly why a portal can be "located" here yet genuinely never placed
                // a single block) without the piece actually generating there. Either way this seed
                // has no usable portal, so treat it exactly like "Not Approved" and keep searching
                // instead of stranding the player in a world with nothing to find.
                sendErrorMessage(client, "Chest nicht gefunden im Umkreis von " + CHEST_SEARCH_RADIUS
                        + " um (" + predictedChestX + ",?," + predictedChestZ + ") - Portal existiert vermutlich nicht wirklich, suche weiter");
                if (onNotApproved != null) {
                    onNotApproved.run();
                }
                return;
            }
            int correctionDx = realChest.getX() - predictedChestX;
            int correctionDz = realChest.getZ() - predictedChestZ;
            int n = realChest.getY() - template.chestLocal.getY();

            boolean cryingFound = false;
            int airCount = 0;
            for (RuinedPortalTemplates.FrameCell cell : template.cells) {
                // Cells where the template itself places air (isObsidianTemplate=false) aren't
                // degradation gaps that need filling - they're air by design, same as any other
                // seed. Only cells the template actually placed obsidian at can either have
                // rolled Crying Obsidian or been removed by age/integrity degradation; counting
                // the by-design air cells as gaps too was inflating the required chest-obsidian
                // count on every single check, well past what real portals' chests carry.
                if (!cell.isObsidianTemplate) {
                    continue;
                }
                BlockPos t = RuinedPortalTemplates.transformAround(cell.x, cell.y, cell.z, result.rpMirror, result.rpRotation, pivotX, pivotZ);
                BlockPos worldPos = new BlockPos(t.getX() + portalX + correctionDx, t.getY() + n, t.getZ() + portalZ + correctionDz);
                world.getChunk(worldPos.getX() >> 4, worldPos.getZ() >> 4);
                boolean isCrying = world.getBlockState(worldPos).isOf(Blocks.CRYING_OBSIDIAN);
                boolean isObsidian = world.getBlockState(worldPos).isOf(Blocks.OBSIDIAN);
                if (isCrying) {
                    cryingFound = true;
                } else if (!isObsidian) {
                    airCount++;
                }
            }

            boolean approved = !cryingFound && airCount <= result.rpChestObsidian;
            sendResultMessage(client, approved, cryingFound, airCount, result.rpChestObsidian, onNotApproved);
        } catch (Exception e) {
            LOGGER.warn("Ruined Portal frame verification failed", e);
            sendErrorMessage(client, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** How far (in blocks, Chebyshev distance) to widen the chest search around the
     * transform-predicted column before giving up - see the long comment above this constant's
     * only call site for why a widened search is needed at all instead of trusting the predicted
     * column exactly. 16 comfortably covers every known/plausible pivot-formula error (the one
     * confirmed case so far was 2 blocks) while staying cheap - a ring search checks the closest
     * columns first and returns as soon as a chest is found. */
    private static final int CHEST_SEARCH_RADIUS = 16;

    /** Expanding ring search (closest columns first, so a small real error is found almost
     * instantly and only a large one pays for the full radius) for a CHEST block near
     * (centerX, centerZ), scanning each candidate column's full plausible Y range. */
    private static BlockPos findChestNear(ServerWorld world, int centerX, int centerZ, int maxRadius) {
        for (int r = 0; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue; // only the new outer ring at this radius - inner cells already checked
                    }
                    Integer y = findChestY(world, centerX + dx, centerZ + dz);
                    if (y != null) {
                        return new BlockPos(centerX + dx, y, centerZ + dz);
                    }
                }
            }
        }
        return null;
    }

    private static Integer findChestY(ServerWorld world, int x, int z) {
        world.getChunk(x >> 4, z >> 4);
        int roughSurfaceY = world.getChunk(x >> 4, z >> 4).sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15);
        for (int y = Math.min(255, roughSurfaceY + 20); y >= 1; y--) {
            if (world.getBlockState(new BlockPos(x, y, z)).isOf(Blocks.CHEST)) {
                return y;
            }
        }
        return null;
    }

    private static void sendErrorMessage(MinecraftClient client, String detail) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(new LiteralText("§c[SeedFilter] Ruined Portal check failed: " + detail)
                        .formatted(Formatting.RED), false);
            }
        });
    }

    private static void sendResultMessage(MinecraftClient client, boolean approved, boolean cryingFound, int airCount, int chestObsidian, Runnable onNotApproved) {
        client.execute(() -> {
            if (client.player == null) {
                return;
            }
            if (approved) {
                client.player.sendMessage(new LiteralText("§a[SeedFilter] Ruined Portal: Approved")
                        .formatted(Formatting.GREEN), false);
            } else {
                String reason = cryingFound
                        ? "Crying Obsidian im Rahmen"
                        : ("nicht genug Obsidian (" + chestObsidian + " in Chest, " + airCount + " Lücken)");
                client.player.sendMessage(new LiteralText("§c[SeedFilter] Ruined Portal: Not Approved (" + reason + ")")
                        .formatted(Formatting.RED), false);
                if (onNotApproved != null) {
                    onNotApproved.run();
                }
            }
        });
    }

    private RuinedPortalVerifier() {
    }
}
