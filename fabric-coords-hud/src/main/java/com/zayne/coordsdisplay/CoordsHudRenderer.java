package com.zayne.coordsdisplay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.LiteralText;

public class CoordsHudRenderer extends DrawableHelper implements HudRenderCallback {

    public static final CoordsHudRenderer INSTANCE = new CoordsHudRenderer();

    private static int lastLeft, lastTop, lastRight, lastBottom;

    private CoordsHudRenderer() {
    }

    @Override
    public void onHudRender(MatrixStack matrixStack, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null) {
            return;
        }
        draw(client, matrixStack, false, -1, -1);
    }

    public void renderInPauseMenu(MatrixStack matrixStack, int mouseX, int mouseY, boolean dragging) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        draw(client, matrixStack, dragging, mouseX, mouseY);
    }

    private void draw(MinecraftClient client, MatrixStack matrixStack, boolean dragging, int mouseX, int mouseY) {
        Entity entity = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;

        String text = String.format(
                "X: %d  Y: %d  Z: %d",
                (int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getY()),
                (int) Math.floor(entity.getZ())
        );

        TextRenderer textRenderer = client.textRenderer;
        int textWidth = textRenderer.getWidth(text);

        HudConfig config = HudConfig.get();
        if (!config.isSet()) {
            config.x = client.getWindow().getScaledWidth() / 2;
            config.y = 10;
        }

        int centerX = config.x;
        int topY = config.y;

        int padding = 3;
        lastLeft = centerX - textWidth / 2 - padding;
        lastRight = centerX + textWidth / 2 + padding;
        lastTop = topY - padding;
        lastBottom = topY + textRenderer.fontHeight + padding;

        if (dragging || (mouseX >= 0 && isInsideBounds(mouseX, mouseY))) {
            fill(matrixStack, lastLeft, lastTop, lastRight, lastBottom, 0x55FFFFFF);
        }

        drawCenteredText(matrixStack, textRenderer, new LiteralText(text), centerX, topY, 0xFFFFFF);
    }

    public static boolean isInsideBounds(int x, int y) {
        return x >= lastLeft && x <= lastRight && y >= lastTop && y <= lastBottom;
    }
}
