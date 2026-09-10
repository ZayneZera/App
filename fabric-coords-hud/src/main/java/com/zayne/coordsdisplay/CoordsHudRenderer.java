package com.zayne.coordsdisplay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;

public class CoordsHudRenderer implements HudRenderCallback {

    @Override
    public void onHudRender(MatrixStack matrixStack, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.options.hudHidden || client.player == null) {
            return;
        }

        Entity entity = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;

        String text = String.format(
                "X: %.1f  Y: %.1f  Z: %.1f",
                entity.getX(), entity.getY(), entity.getZ()
        );

        int screenWidth = client.getWindow().getScaledWidth();
        int centerX = screenWidth / 2;
        int topY = 4;

        DrawableHelper.drawCenteredText(matrixStack, client.textRenderer, new LiteralText(text), centerX, topY, 0xFFFFFF);
    }
}
