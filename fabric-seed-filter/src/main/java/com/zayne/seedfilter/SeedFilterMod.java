package com.zayne.seedfilter;

import com.zayne.seedfilter.mixin.CreateWorldScreenAccessor;
import com.zayne.seedfilter.util.WorldNaming;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;

public class SeedFilterMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
    }

    public static void quickCreateWorld(MinecraftClient client, Screen parent) {
        CreateWorldScreen screen = new CreateWorldScreen(parent);
        client.openScreen(screen);
        ((CreateWorldScreenAccessor) screen).getLevelNameField().setText(WorldNaming.nextName());
    }
}
