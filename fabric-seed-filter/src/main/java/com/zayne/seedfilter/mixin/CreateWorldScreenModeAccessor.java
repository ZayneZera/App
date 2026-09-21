package com.zayne.seedfilter.mixin;

import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * CreateWorldScreen.Mode's own enum constants (SURVIVAL/CREATIVE/HARDCORE) aren't given
 * friendly names in Yarn - only the class itself ("Mode") and this field are. So instead of
 * referencing a constant by name, SeedFilterMod finds the right Mode by scanning
 * CreateWorldScreen.Mode.values() for the one whose defaultGameMode is GameMode.CREATIVE.
 */
@Mixin(CreateWorldScreen.Mode.class)
public interface CreateWorldScreenModeAccessor {
    @Accessor("defaultGameMode")
    GameMode getDefaultGameMode();
}
