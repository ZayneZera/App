package com.zayne.seedfilter.mixin;

import com.zayne.seedfilter.SeedFilterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into Screen (not GameMenuScreen directly) and guards with instanceof, same proven
 * pattern as the coords-display mod's pause-menu hook - GameMenuScreen doesn't reliably
 * expose its own init()/initWidgets() the way we need to target directly.
 */
@Mixin(Screen.class)
public abstract class MixinGameMenuScreen extends Screen {

    protected MixinGameMenuScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedFilter$init(CallbackInfo ci) {
        if (!((Object) this instanceof GameMenuScreen)) {
            return;
        }

        int size = 20;
        int margin = 10;
        int x = this.width - 200 - margin;
        int y = 10 + size + 4;

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
