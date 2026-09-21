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

import java.util.List;

/**
 * Mixes directly into Screen (guarded by instanceof GameMenuScreen). @Shadow declares the
 * members we need since we can't "extends Screen" when the mixin target IS Screen itself.
 */
@Mixin(Screen.class)
public class MixinGameMenuScreen {

    @Shadow
    protected int width;

    @Shadow
    protected <T extends AbstractButtonWidget> T addButton(T button) {
        throw new UnsupportedOperationException("shadowed");
    }

    @Shadow
    public List<?> children() {
        throw new UnsupportedOperationException("shadowed");
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedFilter$init(CallbackInfo ci) {
        if (!((Object) this instanceof GameMenuScreen)) {
            return;
        }

        int lowestY = 0;
        for (Object child : this.children()) {
            if (child instanceof AbstractButtonWidget) {
                int y = ((AbstractButtonWidgetAccessor) child).getY();
                if (y > lowestY) {
                    lowestY = y;
                }
            }
        }

        int x = this.width / 2 - 100;
        int y = lowestY + 24;

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
