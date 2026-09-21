package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.FilterConfig;
import com.zayne.seedfilter.SeedFilterMod;
import com.zayne.seedfilter.scan.ScanStats;
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
    private final ScanStats stats = new ScanStats();
    private final CompletableFuture<SeedScanner.Result> scanFuture;
    private boolean triggered = false;

    public CountdownScreen() {
        super(new LiteralText("Nächster Seed"));
        this.startTimeMillis = System.currentTimeMillis();
        this.scanFuture = SeedScanner.scanAsync(FilterConfig.get(), attempts, cancelled, stats);
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 90, 150, 20,
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
            // Two-step vanilla-style disconnect: tell the world/server first, then tear
            // down the client connection. Only doing the second step previously left the
            // client in a corrupted state (broke Quit, broke the boots button afterward).
            client.execute(() -> {
                if (client.world != null) {
                    client.world.disconnect();
                }
                client.disconnect(new TitleScreen());
                waitThenCreate(client, 3);
            });
        }
    }

    private void waitThenCreate(MinecraftClient client, int settleTicks) {
        if (client.world != null) {
            client.execute(() -> waitThenCreate(client, settleTicks));
        } else if (settleTicks > 0) {
            client.execute(() -> waitThenCreate(client, settleTicks - 1));
        } else {
            scanFuture.thenAccept(result -> client.execute(() ->
                    SeedFilterMod.createAndJoin(client, new TitleScreen(), result.seed, result.spawnX, result.spawnZ)));
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

        renderFunnel(matrices);

        super.render(matrices, mouseX, mouseY, delta);
    }

    /**
     * Live "how many attempts survive each enabled criterion" funnel, so it's obvious which
     * criterion is the actual bottleneck instead of just watching a climbing counter.
     */
    private void renderFunnel(MatrixStack matrices) {
        FilterConfig config = FilterConfig.get();
        int y = this.height / 2 + 25;
        if (config.villageEnabled) {
            y = drawFunnelLine(matrices, "Dorf", stats.passedVillage.get(), stats.reachedVillage.get(), y);
        }
        if (config.ruinedPortalEnabled) {
            y = drawFunnelLine(matrices, "Ruined Portal", stats.passedRuinedPortal.get(), stats.reachedRuinedPortal.get(), y);
        }
        if (config.buriedTreasureEnabled) {
            y = drawFunnelLine(matrices, "Buried Treasure", stats.passedTreasure.get(), stats.reachedTreasure.get(), y);
        }
        if (config.bastionEnabled) {
            y = drawFunnelLine(matrices, "Bastion", stats.passedBastion.get(), stats.reachedBastion.get(), y);
        }
        if (config.fortressEnabled) {
            drawFunnelLine(matrices, "Fortress", stats.passedFortress.get(), stats.reachedFortress.get(), y);
        }
    }

    /** Shows "passed/reached" (not percent-of-total-attempts) so a tiny sample reads as tiny, not as a scary 0.00%. */
    private int drawFunnelLine(MatrixStack matrices, String label, int passed, int reached, int y) {
        String pct = reached > 0 ? String.format(" (%.1f%%)", 100.0 * passed / reached) : "";
        drawCenteredText(matrices, this.textRenderer,
                new LiteralText(label + ": " + passed + "/" + reached + pct), this.width / 2, y, 0x77AAFF);
        return y + 10;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
