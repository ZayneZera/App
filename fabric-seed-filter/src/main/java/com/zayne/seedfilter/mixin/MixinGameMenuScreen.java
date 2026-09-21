package com.zayne.seedfilter.mixin;

import com.zayne.seedfilter.SeedFilterMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class MixinGameMenuScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger("seed-filter");

    protected MixinGameMenuScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedFilter$init(CallbackInfo ci) {
        int buttonY;
        try {
            int lowestY = 0;
            for (Object child : this.children()) {
                if (child instanceof AbstractButtonWidget) {
                    int y = ((AbstractButtonWidgetAccessor) child).getY();
                    if (y > lowestY) {
                        lowestY = y;
                    }
                }
            }
            buttonY = lowestY + 24;
        } catch (Throwable t) {
            LOGGER.warn("seed-filter: failed to find lowest button position, using fallback", t);
            buttonY = this.height - 30;
        }

        try {
            this.addButton(new ButtonWidget(this.width / 2 - 100, buttonY, 200, 20,
                    new LiteralText("Nächster Seed"), button -> {
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
        } catch (Throwable t) {
            LOGGER.error("seed-filter: failed to add Nächster Seed button", t);
        }
    }
}
