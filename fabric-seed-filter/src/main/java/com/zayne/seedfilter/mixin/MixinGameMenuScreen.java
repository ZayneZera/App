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
 * Confirmed-working pattern (matches known-good examples): mixin directly into
 * GameMenuScreen, extending Screen for access to addButton/width/height. Fixed position,
 * no dynamic lookup - that repeatedly broke visibility in earlier attempts.
 */
@Mixin(GameMenuScreen.class)
public abstract class MixinGameMenuScreen extends Screen {

    protected MixinGameMenuScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedFilter$init(CallbackInfo ci) {
        int x = this.width / 2 - 100;
        int y = this.height / 4 + 128;

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
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
                client.execute(() -> SeedFilterMod.startScanAndCreate(client, new TitleScreen()));
            }, "seed-filter-disconnect-wait").start();
        }));
    }
}
