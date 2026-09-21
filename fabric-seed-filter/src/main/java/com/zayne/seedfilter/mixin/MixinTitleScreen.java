package com.zayne.seedfilter.mixin;

import com.zayne.seedfilter.SeedFilterMod;
import com.zayne.seedfilter.gui.SeedFilterSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(net.minecraft.text.Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seedFilter$init(CallbackInfo ci) {
        int size = 32;
        int x = this.width - size - 10;
        int y = 10;

        this.addButton(new BootsButton(x, y, size, button ->
                SeedFilterMod.quickCreateWorld(MinecraftClient.getInstance(), (Screen) (Object) this)));

        this.addButton(new ButtonWidget(x, y + size + 4, size, 20, new LiteralText("Filter"), button ->
                MinecraftClient.getInstance().openScreen(new SeedFilterSettingsScreen((Screen) (Object) this))));
    }

    private static class BootsButton extends ButtonWidget {
        BootsButton(int x, int y, int size, PressAction onPress) {
            super(x, y, size, size, LiteralText.EMPTY, onPress);
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderButton(matrices, mouseX, mouseY, delta);
            ItemStack stack = new ItemStack(Items.NETHERITE_BOOTS);
            MinecraftClient client = MinecraftClient.getInstance();
            int itemX = this.x + (this.width - 16) / 2;
            int itemY = this.y + (this.height - 16) / 2;
            client.getItemRenderer().renderGuiItemIcon(stack, itemX, itemY);
        }
    }
}
