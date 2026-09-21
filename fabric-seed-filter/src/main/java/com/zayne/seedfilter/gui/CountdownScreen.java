package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.FilterConfig;
import com.zayne.seedfilter.SeedFilterMod;
import com.zayne.seedfilter.scan.SeedScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shown when "Nächster Seed" is clicked in the pause menu. Starts scanning for a matching
 * seed IMMEDIATELY (while still in the current world) and shows a 3-second countdown with
 * a Cancel button. If not cancelled, once the countdown ends it disconnects to the title
 * screen, waits briefly, then auto-creates/joins the (by then likely already found) seed.
 * Scanning up front, before any disconnect happens, avoids the disconnect/scan race that
 * broke the previous approach.
 */
public class CountdownScreen extends Screen {

    private static final int COUNTDOWN_SECONDS = 3;

    private final long startTimeMillis;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicInteger attempts = new AtomicInteger();
    private final CompletableFuture<SeedScanner.Result> scanFuture;
    private boolean triggered = false;

    public CountdownScreen() {
        super(new LiteralText("Nächster Seed"));
        this.startTimeMillis = System.currentTimeMillis();
        this.scanFuture = SeedScanner.scanAsync(FilterConfig.get(), attempts, cancelled);
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 50, 150, 20,
                new LiteralText("Abbrechen"), button -> {
            cancelled.set(true);
            this.client.openScreen(null);
        }));
    }

    @Override
    public void tick() {
        super.tick();
        if (cancelled.get() || triggered) {
            return;
        }
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        if (elapsedMillis >= COUNTDOWN_SECONDS * 1000L) {
            triggered = true;
            MinecraftClient client = MinecraftClient.getInstance();
            // Defer via the task queue instead of calling disconnect() directly from
            // inside tick() - doing it inline while this screen's own tick is still on
            // the call stack corrupted client state (broke Quit, broke the boots button
            // afterward too). Deferring lets this tick() call return first.
            client.execute(() -> client.disconnect(new TitleScreen()));
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        int remaining = Math.max(1, COUNTDOWN_SECONDS - (int) (elapsedMillis / 1000));

        matrices.push();
        matrices.translate(this.width / 2.0, this.height / 2.0 - 40, 0);
        matrices.scale(4f, 4f, 4f);
        drawCenteredText(matrices, this.textRenderer, new LiteralText(String.valueOf(remaining)), 0, 0, 0xFFFFFF);
        matrices.pop();

        drawCenteredText(matrices, this.textRenderer, new LiteralText("Suche Seed... Versuche: " + attempts.get()),
                this.width / 2, this.height / 2 + 10, 0xAAAAAA);

        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
