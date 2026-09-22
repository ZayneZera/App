package com.zayne.seedfilter;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Browser-style back/forward history for this mod's own screens (the settings menu and the whole
 * seed bank family) - MB5 (mouse button index 4) goes back, MB4 (index 3) goes forward into
 * whatever screen you just came back from, exactly like a browser's Alt+Left/Right. Call
 * {@link #navigate} instead of client.openScreen() whenever moving from one of our screens to
 * another, and {@link #handleNavClick} at the very top of every participating screen's
 * mouseClicked() override.
 */
public final class MenuNav {

    private static final Deque<Screen> back = new ArrayDeque<>();
    private static final Deque<Screen> forward = new ArrayDeque<>();

    /** Pushes the current screen onto the back stack, clears the forward stack (a fresh forward
     * navigation invalidates whatever "redo" path existed before it - standard browser history
     * behavior), then opens {@code to}. */
    public static void navigate(MinecraftClient client, Screen to) {
        if (client.currentScreen != null) {
            back.push(client.currentScreen);
        }
        forward.clear();
        client.openScreen(to);
    }

    /**
     * Handles a mouseClicked's button if it's MB4/MB5, returning true if it did (caller should
     * return true immediately without running its own click logic for that event).
     *
     * @param fallbackBack runs when MB5 is pressed but the back stack is empty - happens the very
     *                      first time a screen was opened from outside MenuNav (a hotkey, a
     *                      title-screen button) rather than via navigate() from another of our
     *                      screens. Should do whatever that screen's own back/close action does.
     *                      May be null if there's nothing sensible to fall back to.
     */
    public static boolean handleNavClick(MinecraftClient client, int button, Runnable fallbackBack) {
        if (button == 4) {
            if (!back.isEmpty()) {
                if (client.currentScreen != null) {
                    forward.push(client.currentScreen);
                }
                client.openScreen(back.pop());
            } else if (fallbackBack != null) {
                fallbackBack.run();
            }
            return true;
        }
        if (button == 3) {
            if (!forward.isEmpty()) {
                if (client.currentScreen != null) {
                    back.push(client.currentScreen);
                }
                client.openScreen(forward.pop());
            }
            return true;
        }
        return false;
    }

    private MenuNav() {
    }
}
