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
            client.execute(() -> createAndJoin(client, titleScreen, result.seed));
        });
    }

    private static void createAndJoin(MinecraftClient client, Screen titleScreen, long seed) {
        CreateWorldScreen screen = new CreateWorldScreen(titleScreen);
        client.openScreen(screen);

        CreateWorldScreenAccessor accessor = (CreateWorldScreenAccessor) screen;
        accessor.getLevelNameField().setText(WorldNaming.nextName());

        MoreOptionsDialog dialog = accessor.getMoreOptionsDialog();
        ((MoreOptionsDialogAccessor) dialog).getSeedTextField().setText(String.valueOf(seed));

        accessor.invokeCreateLevel();
    }
}
