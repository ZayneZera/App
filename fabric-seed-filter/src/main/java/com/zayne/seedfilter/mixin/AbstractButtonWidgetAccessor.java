package com.zayne.seedfilter.mixin;

import net.minecraft.client.gui.widget.AbstractButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractButtonWidget.class)
public interface AbstractButtonWidgetAccessor {
    @Accessor("y")
    int getY();
}
