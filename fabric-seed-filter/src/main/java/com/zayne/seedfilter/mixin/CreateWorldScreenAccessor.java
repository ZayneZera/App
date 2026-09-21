package com.zayne.seedfilter.mixin;

import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccessor {
    @Accessor("levelNameField")
    TextFieldWidget getLevelNameField();

    @Accessor("moreOptionsDialog")
    MoreOptionsDialog getMoreOptionsDialog();

    @Accessor("cheatsEnabled")
    void setCheatsEnabled(boolean cheatsEnabled);

    // currentMode's real type is the package-private CreateWorldScreen.Mode, which our mod's
    // package can't reference directly (compile error: "Mode is not public in
    // CreateWorldScreen"). Object works fine for a Mixin accessor - it's plain bytecode field
    // access under the hood, not a Java-level type check - and the caller uses reflection to
    // work with the actual value (see SeedFilterMod.applyCreativeDefault).
    @Accessor("currentMode")
    Object getCurrentMode();

    @Accessor("currentMode")
    void setCurrentMode(Object mode);

    @Invoker("createLevel")
    void invokeCreateLevel();
}
