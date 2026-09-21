package com.zayne.seedfilter.mixin;

import com.zayne.seedfilter.SeedFilterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes directly into Screen (guarded by instanceof GameMenuScreen). Since the target IS
 * Screen itself (not a subclass of it), we can't use the "extends Screen" trick (that would
 * be a class extending itself after merging) - @Shadow declares the members we need instead.
 */
@Mixin(Screen.class)
public class MixinGameMenuScreen {

    @Shadow
    protected int width;

    @Shadow
    protected <T extends AbstractButtonWidget> T addButton(T button) {
        throw new UnsupportedOperationException("shadowed");
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedFilter$init(CallbackInfo ci) {
        if (!((Object) this instanceof GameMenuScreen)) {
            return;
        }

        int margin = 10;
        int x = this.width - 200 - margin;
        int y = 10 + 20 + 4;

        this.addButton(new ButtonWidget(x, y, 200, 20, new LiteralText("Nächster Seed"), button -> {
            MinecraftClient client = MinecraftClient.getInstance();
            TitleScreen title = new TitleScreen();
            client.disconnect(title);

            new Thread(() -> {
                while (client.world != null) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ignored) {
                    }
                }
                client.execute(() -> SeedFilterMod.startScanAndCreate(client, title));
            }, "seed-filter-disconnect-wait").start();
        }));
    }
}
