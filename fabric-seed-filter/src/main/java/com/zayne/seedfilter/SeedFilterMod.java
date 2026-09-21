package com.zayne.seedfilter;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;

public class SeedFilterMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
    }

    public static void quickCreateWorld(MinecraftClient client, Screen parent) {
        client.openScreen(new CreateWorldScreen(parent));
    }
}
