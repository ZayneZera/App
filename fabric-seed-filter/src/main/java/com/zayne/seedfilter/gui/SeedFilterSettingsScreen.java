package com.zayne.seedfilter.gui;

import com.zayne.seedfilter.FilterConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SeedFilterSettingsScreen extends Screen {

    private final Screen parent;
    private final FilterConfig config;

    private final List<TextFieldWidget> intFields = new ArrayList<>();
    private final Map<TextFieldWidget, Consumer<Integer>> intFieldSetters = new HashMap<>();
    private final List<int[]> sectionLabelYs = new ArrayList<>();
    private final List<String> sectionLabelTexts = new ArrayList<>();
    private final List<int[]> rowLabelYs = new ArrayList<>();
    private final List<String> rowLabelTexts = new ArrayList<>();

    private int nextY;

    public SeedFilterSettingsScreen(Screen parent) {
        super(new LiteralText("Seed-Filter Einstellungen"));
        this.parent = parent;
        this.config = FilterConfig.get();
    }

    @Override
    protected void init() {
        this.intFields.clear();
        this.intFieldSetters.clear();
        this.sectionLabelYs.clear();
        this.sectionLabelTexts.clear();
        this.rowLabelYs.clear();
        this.rowLabelTexts.clear();
        this.nextY = 30;

        int centerX = this.width / 2;

        addSectionLabel(centerX, "Ruined Portal");
        addToggleRow(centerX, "Aktiv", config.ruinedPortalEnabled, v -> config.ruinedPortalEnabled = v);
        addIntRow(centerX, "Max. Chunks vom Spawn", config.ruinedPortalMaxChunks, v -> config.ruinedPortalMaxChunks = v);
        addToggleRow(centerX, "Looting-2/3-Schwert (langsam!)", config.ruinedPortalRequireLootingSword, v -> config.ruinedPortalRequireLootingSword = v);

        addSectionLabel(centerX, "Village");
        addToggleRow(centerX, "Aktiv", config.villageEnabled, v -> config.villageEnabled = v);
        addIntRow(centerX, "Max. Chunks vom Spawn", config.villageMaxChunks, v -> config.villageMaxChunks = v);

        addSectionLabel(centerX, "Buried Treasure");
        addToggleRow(centerX, "Aktiv", config.buriedTreasureEnabled, v -> config.buriedTreasureEnabled = v);
        addIntRow(centerX, "Max. Chunks vom Spawn", config.buriedTreasureMaxChunks, v -> config.buriedTreasureMaxChunks = v);

        addSectionLabel(centerX, "Bastion");
        addToggleRow(centerX, "Aktiv", config.bastionEnabled, v -> config.bastionEnabled = v);
        addToggleRow(centerX, "Bridge erlaubt", config.bastionAllowBridge, v -> config.bastionAllowBridge = v);
        addToggleRow(centerX, "Housing erlaubt", config.bastionAllowHousing, v -> config.bastionAllowHousing = v);
        addToggleRow(centerX, "Stables erlaubt", config.bastionAllowStables, v -> config.bastionAllowStables = v);
        addToggleRow(centerX, "Treasure erlaubt", config.bastionAllowTreasure, v -> config.bastionAllowTreasure = v);
        addIntRow(centerX, "Max. Nether-Chunks (Spawn/8)", config.bastionMaxNetherChunks, v -> config.bastionMaxNetherChunks = v);

        addSectionLabel(centerX, "Fortress");
        addToggleRow(centerX, "Aktiv", config.fortressEnabled, v -> config.fortressEnabled = v);
        addIntRow(centerX, "Max. Nether-Chunks (Spawn/8)", config.fortressMaxNetherChunks, v -> config.fortressMaxNetherChunks = v);

        this.addButton(new ButtonWidget(centerX - 100, this.height - 28, 200, 20, new LiteralText("Fertig"), button -> {
            saveAll();
            this.client.openScreen(this.parent);
        }));
    }

    private void addSectionLabel(int centerX, String text) {
        this.nextY += 6;
        this.sectionLabelTexts.add(text);
        this.sectionLabelYs.add(new int[]{centerX - 150, this.nextY});
        this.nextY += 12;
    }

    private void addToggleRow(int centerX, String label, boolean initial, Consumer<Boolean> setter) {
        int y = this.nextY;
        this.rowLabelTexts.add(label);
        this.rowLabelYs.add(new int[]{centerX - 150, y + 5});

        boolean[] state = {initial};
        ButtonWidget button = new ButtonWidget(centerX + 10, y, 100, 18,
                new LiteralText(state[0] ? "AN" : "AUS"), b -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            b.setMessage(new LiteralText(state[0] ? "AN" : "AUS"));
        });
        this.addButton(button);
        this.nextY += 20;
    }

    private void addIntRow(int centerX, String label, int initial, Consumer<Integer> setter) {
        int y = this.nextY;
        this.rowLabelTexts.add(label);
        this.rowLabelYs.add(new int[]{centerX - 150, y + 5});

        TextFieldWidget field = new TextFieldWidget(this.textRenderer, centerX + 10, y, 60, 18, new LiteralText(label));
        field.setText(String.valueOf(initial));
        field.setMaxLength(4);
        this.addChild(field);
        this.intFields.add(field);
        this.intFieldSetters.put(field, setter);
        this.nextY += 20;
    }

    private void saveAll() {
        for (TextFieldWidget field : intFields) {
            Consumer<Integer> setter = intFieldSetters.get(field);
            if (setter == null) {
                continue;
            }
            try {
                setter.accept(Integer.parseInt(field.getText().trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        config.save();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        for (int i = 0; i < sectionLabelTexts.size(); i++) {
            int[] pos = sectionLabelYs.get(i);
            drawStringWithShadow(matrices, this.textRenderer, sectionLabelTexts.get(i), pos[0], pos[1], 0xFFD700);
        }
        for (int i = 0; i < rowLabelTexts.size(); i++) {
            int[] pos = rowLabelYs.get(i);
            drawStringWithShadow(matrices, this.textRenderer, rowLabelTexts.get(i), pos[0], pos[1], 0xFFFFFF);
        }

        super.render(matrices, mouseX, mouseY, delta);
    }
}
