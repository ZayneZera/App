package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.ExternalEngine;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

/** Shown instead of starting a search when seedfilter.exe hasn't been placed in the game dir yet. */
public class EngineMissingScreen extends Screen {

    private final Screen parent;

    public EngineMissingScreen(Screen parent) {
        super(new LiteralText("seedfilter.exe fehlt"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addButton(new ButtonWidget(this.width / 2 - 100, this.height / 2 + 30, 200, 20,
                new LiteralText("Zurück"), button -> this.client.openScreen(this.parent)));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, this.height / 2 - 40, 0xFF5555);
        drawCenteredText(matrices, this.textRenderer,
                new LiteralText("Lege seedfilter.exe hier ab:"), this.width / 2, this.height / 2 - 15, 0xFFFFFF);
        drawCenteredText(matrices, this.textRenderer,
                new LiteralText(ExternalEngine.getExePath().toString()), this.width / 2, this.height / 2, 0xAAAAAA);
        super.render(matrices, mouseX, mouseY, delta);
    }
}
