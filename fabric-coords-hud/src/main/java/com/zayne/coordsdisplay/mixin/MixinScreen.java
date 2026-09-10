package com.zayne.coordsdisplay.mixin;

import com.zayne.coordsdisplay.CoordsHudRenderer;
import com.zayne.coordsdisplay.HudConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {

    private boolean coordsDisplay$dragging = false;
    private boolean coordsDisplay$wasMouseDown = false;
    private int coordsDisplay$offsetX;
    private int coordsDisplay$offsetY;

    @Inject(method = "render", at = @At("TAIL"))
    private void coordsDisplay$render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof GameMenuScreen)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        boolean mouseDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;

        if (mouseDown && !coordsDisplay$wasMouseDown && CoordsHudRenderer.isInsideBounds(mouseX, mouseY)) {
            coordsDisplay$dragging = true;
            HudConfig config = HudConfig.get();
            coordsDisplay$offsetX = mouseX - config.x;
            coordsDisplay$offsetY = mouseY - config.y;
        } else if (mouseDown && coordsDisplay$dragging) {
            HudConfig config = HudConfig.get();
            config.x = mouseX - coordsDisplay$offsetX;
            config.y = mouseY - coordsDisplay$offsetY;
        } else if (!mouseDown && coordsDisplay$dragging) {
            coordsDisplay$dragging = false;
            HudConfig.get().save();
        }
        coordsDisplay$wasMouseDown = mouseDown;

        CoordsHudRenderer.INSTANCE.renderInPauseMenu(matrices, mouseX, mouseY, coordsDisplay$dragging);
    }
}
