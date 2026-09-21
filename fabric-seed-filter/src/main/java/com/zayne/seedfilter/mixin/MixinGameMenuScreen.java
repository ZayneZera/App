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
 * Confirmed-working pattern: mixin directly into GameMenuScreen, extending Screen for
 * access to addButton/width/height. Fixed position (dynamic lookup broke visibility
 * repeatedly). Click handling waits for disconnect by re-queuing itself on the client
 * thread via MinecraftClient.execute() instead of a background Thread - reading
 * client.world from an unsynchronized background thread risked never observing the
 * update (classic cross-thread visibility bug), and a plain non-daemon Thread stuck in
 * that loop also prevented the JVM from exiting when the game was closed.
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
            waitForDisconnect(client, 10);
        }));
    }

    private static void waitForDisconnect(MinecraftClient client, int settleTicks) {
        if (client.world != null) {
            client.execute(() -> waitForDisconnect(client, settleTicks));
        } else if (settleTicks > 0) {
            client.execute(() -> waitForDisconnect(client, settleTicks - 1));
        } else {
            SeedFilterMod.startScanAndCreate(client, new TitleScreen());
        }
    }
}
