package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.concurrent.atomic.AtomicReference;

public class ScanProgressScreen extends Screen {

    private final Screen parent;
    private final AtomicReference<Process> processHolder;
    private final long startTimeMillis;

    public ScanProgressScreen(Screen parent, AtomicReference<Process> processHolder) {
        super(new LiteralText("Suche passende Seed..."));
        this.parent = parent;
        this.processHolder = processHolder;
        this.startTimeMillis = System.currentTimeMillis();
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 20, 150, 20,
                new LiteralText("Abbrechen"), button -> {
            ExternalEngine.cancel(processHolder);
            this.client.openScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        long elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
        drawCenteredText(matrices, this.textRenderer, new LiteralText("Läuft seit " + elapsedSeconds + "s..."),
                this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
