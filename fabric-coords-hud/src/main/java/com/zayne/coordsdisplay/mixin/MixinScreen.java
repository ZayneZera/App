package com.zayne.coordsdisplay.mixin;

import com.zayne.coordsdisplay.CoordsHudRenderer;
import com.zayne.coordsdisplay.HudConfig;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class MixinScreen {

    private boolean coordsDisplay$dragging = false;
    private int coordsDisplay$offsetX;
    private int coordsDisplay$offsetY;

    @Inject(method = "render", at = @At("TAIL"))
    private void coordsDisplay$render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof GameMenuScreen) {
            CoordsHudRenderer.INSTANCE.renderInPauseMenu(matrices, mouseX, mouseY, coordsDisplay$dragging);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void coordsDisplay$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof GameMenuScreen && button == 0
                && CoordsHudRenderer.isInsideBounds((int) mouseX, (int) mouseY)) {
            coordsDisplay$dragging = true;
            HudConfig config = HudConfig.get();
            coordsDisplay$offsetX = (int) mouseX - config.x;
            coordsDisplay$offsetY = (int) mouseY - config.y;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void coordsDisplay$mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (coordsDisplay$dragging) {
            HudConfig config = HudConfig.get();
            config.x = (int) mouseX - coordsDisplay$offsetX;
            config.y = (int) mouseY - coordsDisplay$offsetY;
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void coordsDisplay$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (coordsDisplay$dragging) {
            coordsDisplay$dragging = false;
            HudConfig.get().save();
            cir.setReturnValue(true);
        }
    }
}
