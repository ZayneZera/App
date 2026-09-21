package com.zayne.seedfilter.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ScanProgressScreen extends Screen {

    private final Screen parent;
    private final AtomicInteger attempts;
    private final AtomicBoolean cancelled;

    public ScanProgressScreen(Screen parent, AtomicInteger attempts, AtomicBoolean cancelled) {
        super(new LiteralText("Suche passende Seed..."));
        this.parent = parent;
        this.attempts = attempts;
        this.cancelled = cancelled;
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 75, this.height / 2 + 20, 150, 20,
                new LiteralText("Abbrechen"), button -> {
            cancelled.set(true);
            this.client.openScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer, "Versuche: " + attempts.get(), this.width / 2, this.height / 2 - 10, 0xAAAAAA);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
