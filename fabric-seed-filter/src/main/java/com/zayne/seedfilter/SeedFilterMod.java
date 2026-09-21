package com.zayne.seedfilter;

import com.zayne.seedfilter.gui.ScanProgressScreen;
import com.zayne.seedfilter.mixin.CreateWorldScreenAccessor;
import com.zayne.seedfilter.mixin.MoreOptionsDialogAccessor;
import com.zayne.seedfilter.scan.SeedScanner;
import com.zayne.seedfilter.util.WorldNaming;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.text.LiteralText;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SeedFilterMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
    }

    public static void startScanAndCreate(MinecraftClient client, Screen titleScreen) {
        FilterConfig config = FilterConfig.get();
        AtomicInteger attempts = new AtomicInteger();
        AtomicBoolean cancelled = new AtomicBoolean(false);

        client.openScreen(new ScanProgressScreen(titleScreen, attempts, cancelled));

        SeedScanner.scanAsync(config, attempts, cancelled).thenAccept(result -> {
            if (cancelled.get()) {
                return;
            }
            client.execute(() -> createAndJoin(client, titleScreen, result.seed, result.spawnX, result.spawnZ));
        });
    }

    public static void createAndJoin(MinecraftClient client, Screen titleScreen, long seed, int spawnX, int spawnZ) {
        CreateWorldScreen screen = new CreateWorldScreen(titleScreen);
        client.openScreen(screen);

        CreateWorldScreenAccessor accessor = (CreateWorldScreenAccessor) screen;
        accessor.getLevelNameField().setText(WorldNaming.nextName());

        MoreOptionsDialog dialog = accessor.getMoreOptionsDialog();
        ((MoreOptionsDialogAccessor) dialog).getSeedTextField().setText(String.valueOf(seed));

        if (FilterConfig.get().enableCheats) {
            accessor.setCheatsEnabled(true);
        }

        accessor.invokeCreateLevel();
        announceComputedSpawn(client, spawnX, spawnZ);
    }

    /**
     * Polls (via self-requeuing client.execute, same pattern as the countdown's world-teardown
     * wait) until the player has actually spawned in, then posts the spawn position our headless
     * search computed - so it can be compared directly against F3's real coordinates in-game.
     */
    private static void announceComputedSpawn(MinecraftClient client, int spawnX, int spawnZ) {
        client.execute(() -> {
            if (client.player == null) {
                announceComputedSpawn(client, spawnX, spawnZ);
                return;
            }
            client.player.sendMessage(new LiteralText(
                    "§e[SeedFilter] Berechneter Spawn: X=" + spawnX + " Z=" + spawnZ
                            + "  (mit F3 vergleichen)"), false);
        });
    }
}
