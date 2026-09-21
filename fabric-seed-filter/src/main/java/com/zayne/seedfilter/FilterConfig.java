package com.zayne.seedfilter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("seed-filter.json");
    private static FilterConfig instance;

    // Ruined Portal
    public boolean ruinedPortalEnabled = true;
    public int ruinedPortalMaxChunks = 5;
    public boolean ruinedPortalRequireLootingSword = false;

    // Village
    public boolean villageEnabled = true;
    public int villageMaxChunks = 8;

    // Buried Treasure
    public boolean buriedTreasureEnabled = true;
    public int buriedTreasureMaxChunks = 4;

    // Bastion (distance measured in nether chunks from overworld-spawn/8)
    public boolean bastionEnabled = true;
    public boolean bastionAllowBridge = true;
    public boolean bastionAllowHousing = true;
    public boolean bastionAllowStables = true;
    public boolean bastionAllowTreasure = true;
    public int bastionMaxNetherChunks = 6;

    // Fortress (distance measured in nether chunks from overworld-spawn/8)
    public boolean fortressEnabled = true;
    public int fortressMaxNetherChunks = 12;

    // World creation
    public boolean enableCheats = false;

    public static FilterConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static FilterConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                FilterConfig loaded = GSON.fromJson(reader, FilterConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException ignored) {
            }
        }
        return new FilterConfig();
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
