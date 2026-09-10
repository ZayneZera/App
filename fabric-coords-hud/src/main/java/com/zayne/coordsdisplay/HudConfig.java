package com.zayne.coordsdisplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("coords-display.json");
    private static HudConfig instance;

    public int x = Integer.MIN_VALUE;
    public int y = Integer.MIN_VALUE;

    public static HudConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public boolean isSet() {
        return x != Integer.MIN_VALUE && y != Integer.MIN_VALUE;
    }

    private static HudConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                HudConfig loaded = GSON.fromJson(reader, HudConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException ignored) {
            }
        }
        return new HudConfig();
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }
}
