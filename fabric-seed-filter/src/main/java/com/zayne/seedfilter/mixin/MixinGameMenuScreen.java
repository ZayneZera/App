package com.zayne.seedfilter.mixin;

import com.zayne.seedfilter.gui.CountdownScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin directly into GameMenuScreen, extending Screen for access to addButton/width/height
 * (confirmed-working pattern). Fixed position. Clicking opens CountdownScreen, which owns
 * all the scan/disconnect/create logic.
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

        this.addButton(new ButtonWidget(x, y, 200, 20, new LiteralText("Nächster Seed"), button ->
                MinecraftClient.getInstance().openScreen(new CountdownScreen())));
    }
}
