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

import java.util.Arrays;

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
     * portal placement for this seed - matches the "only when I have the check enabled" ask. */
    public static void verifyAndAnnounce(MinecraftClient client, ExternalEngine.Result result, SeedFilterConfig config) {
        if (!config.ruinedPortalFrameCheck || !result.rpFound) {
            LOGGER.info("Ruined Portal verification skipped: frameCheck={} rpFound={}", config.ruinedPortalFrameCheck, result.rpFound);
            return;
        }
        LOGGER.info("Ruined Portal verification starting: portal=({},{}) template={} rotation={} mirror={} chestObsidian={}",
                result.rpPortalX, result.rpPortalZ, result.rpTemplateIndex, result.rpRotation, result.rpMirror, result.rpChestObsidian);
        pollUntilReady(client, result, 5);
    }

    private static void pollUntilReady(MinecraftClient client, ExternalEngine.Result result, int settleTicks) {
        client.execute(() -> {
            if (client.player == null || client.getServer() == null) {
                pollUntilReady(client, result, settleTicks);
                return;
            }
            if (settleTicks > 0) {
                pollUntilReady(client, result, settleTicks - 1);
                return;
            }
            MinecraftServer server = client.getServer();
            server.execute(() -> runCheckOnServerThread(client, server, result));
        });
    }

    private static void runCheckOnServerThread(MinecraftClient client, MinecraftServer server, ExternalEngine.Result result) {
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

            // Force-generate/load every chunk the frame or the corner-height sample touches
            // before reading anything from them.
            int[] cornerLocalX = {0, template.sizeX - 1, 0, template.sizeX - 1};
            int[] cornerLocalZ = {0, 0, template.sizeZ - 1, template.sizeZ - 1};
            int[] cornerHeights = new int[4];
            for (int i = 0; i < 4; i++) {
                BlockPos t = RuinedPortalTemplates.transformAround(cornerLocalX[i], 0, cornerLocalZ[i], result.rpMirror, result.rpRotation, pivotX, pivotZ);
                int wx = t.getX() + portalX;
                int wz = t.getZ() + portalZ;
                cornerHeights[i] = world.getChunk(wx >> 4, wz >> 4).sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, wx & 15, wz & 15) - 1;
            }
            // 3rd-highest (2nd-lowest) of the 4 corners - same order statistic as vanilla's
            // "at least 3 of 4 corners have solid ground" scan, now off exact heightmap data
            // instead of an approximation.
            Arrays.sort(cornerHeights);
            int n = cornerHeights[1];
            if (n < 15) n = 15;

            boolean cryingFound = false;
            int airCount = 0;
            for (RuinedPortalTemplates.FrameCell cell : template.cells) {
                BlockPos t = RuinedPortalTemplates.transformAround(cell.x, cell.y, cell.z, result.rpMirror, result.rpRotation, pivotX, pivotZ);
                BlockPos worldPos = new BlockPos(t.getX() + portalX, t.getY() + n, t.getZ() + portalZ);
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
            sendResultMessage(client, approved, cryingFound, airCount, result.rpChestObsidian);
        } catch (Exception e) {
            LOGGER.warn("Ruined Portal frame verification failed", e);
            sendErrorMessage(client, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void sendErrorMessage(MinecraftClient client, String detail) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(new LiteralText("§c[SeedFilter] Ruined Portal check failed: " + detail)
                        .formatted(Formatting.RED), false);
            }
        });
    }

    private static void sendResultMessage(MinecraftClient client, boolean approved, boolean cryingFound, int airCount, int chestObsidian) {
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
            }
        });
    }

    private RuinedPortalVerifier() {
    }
}
