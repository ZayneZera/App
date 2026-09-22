package com.zayne.seedfilter.mixin;

import net.minecraft.client.gui.widget.AbstractButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractButtonWidget.class)
public interface AbstractButtonWidgetAccessor {
    @Accessor("y")
    int getY();

    /** Lets the settings menu hide (and, since AbstractButtonWidget's own click handling checks
     * this same field, disable) any row widget that's scrolled outside the visible panel area -
     * without this, a checkbox/slider scrolled past the frame stayed fully clickable even though
     * its label text (drawn separately, with its own bounds check) had already disappeared. */
    @Accessor("visible")
    void setVisible(boolean visible);
}
