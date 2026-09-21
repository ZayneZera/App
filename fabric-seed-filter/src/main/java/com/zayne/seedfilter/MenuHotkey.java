package com.zayne.seedfilter;

import com.zayne.seedfilter.gui.SeedFilterMenuScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Ctrl+M opens/closes the in-game settings menu from anywhere (title screen, pause menu, or
 * straight out of gameplay) - polled every client tick rather than tied to a single screen's
 * key handling, so it works no matter what's currently open.
 */
public class MenuHotkey {

    private static boolean wasDown = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(MenuHotkey::tick);
    }

    private static void tick(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        boolean ctrl = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean m = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_M);
        boolean down = ctrl && m;

        if (down && !wasDown) {
            toggle(client);
        }
        wasDown = down;
    }

    private static void toggle(MinecraftClient client) {
        Screen current = client.currentScreen;
        if (current instanceof SeedFilterMenuScreen) {
            client.openScreen(((SeedFilterMenuScreen) current).getBackgroundScreen());
        } else {
            client.openScreen(new SeedFilterMenuScreen(current));
        }
    }
}
