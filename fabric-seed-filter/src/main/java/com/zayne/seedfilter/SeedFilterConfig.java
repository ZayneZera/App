package com.zayne.seedfilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Plain key=value config, matching seed-filter-engine/src/config.c exactly (same file format,
 * same keys) - this mod's in-game menu and the external exe's headless search both read/write
 * the same seedfilter.cfg.
 */
public class SeedFilterConfig {
    public boolean villageEnabled = true;
    public int villageMaxChunks = 8;

    public boolean ruinedPortalEnabled = true;
    public int ruinedPortalMaxChunks = 5;
    public boolean ruinedPortalLootingSword = false;
    public boolean ruinedPortalFrameCheck = false;

    public boolean buriedTreasureEnabled = true;
    public int buriedTreasureMaxChunks = 4;
    public int buriedTreasureMinTnt = 0;
    public boolean buriedTreasureDiamondFilter = false;
    public boolean buriedTreasureIronFilter = false;
    public int buriedTreasureMinFish = 4;

    public boolean bastionEnabled = true;
    public boolean bastionAllowBridge = true;
    public boolean bastionAllowHousing = true;
    public boolean bastionAllowStables = true;
    public boolean bastionAllowTreasure = true;
    public int bastionMaxNetherChunks = 6;

    public boolean fortressEnabled = true;
    public int fortressMaxNetherChunks = 12;

    public boolean enableCheats = false;
    public boolean creativeMode = false;
    /** Not written by save() (see there) - there's no in-game UI for this, so leaving it out lets
     * the exe's own config_set_defaults() auto-detect the real CPU core count every run instead
     * of this stale Java-side default silently overwriting that the moment any other setting is
     * saved from the menu. Kept only so an externally-set thread_count= in the file round-trips
     * instead of being silently dropped if something else ever reads this field. */
    public int threadCount = 6;

    /** Global AND/OR switch across the five main categories (village/ruinedPortal/buriedTreasure/
     * bastion/fortress) - see seed-filter-engine's cfg->orMode. Only matters for the seed bank's
     * background search; the normal join-now search always uses AND (a "some also work" seed
     * wouldn't make sense to auto-join). */
    public boolean orMode = false;

    public static SeedFilterConfig load(Path path) {
        SeedFilterConfig cfg = new SeedFilterConfig();
        if (!Files.exists(path)) {
            return cfg;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                int value;
                try {
                    value = Integer.parseInt(line.substring(eq + 1).trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                cfg.apply(key, value);
            }
        } catch (IOException ignored) {
        }
        return cfg;
    }

    private void apply(String key, int value) {
        switch (key) {
            case "village_enabled": villageEnabled = value != 0; break;
            case "village_max_chunks": villageMaxChunks = value; break;
            case "ruined_portal_enabled": ruinedPortalEnabled = value != 0; break;
            case "ruined_portal_max_chunks": ruinedPortalMaxChunks = value; break;
            case "ruined_portal_looting_sword": ruinedPortalLootingSword = value != 0; break;
            case "ruined_portal_frame_check": ruinedPortalFrameCheck = value != 0; break;
            case "buried_treasure_enabled": buriedTreasureEnabled = value != 0; break;
            case "buried_treasure_max_chunks": buriedTreasureMaxChunks = value; break;
            case "buried_treasure_min_tnt": buriedTreasureMinTnt = value; break;
            case "buried_treasure_diamond_filter": buriedTreasureDiamondFilter = value != 0; break;
            case "buried_treasure_iron_filter": buriedTreasureIronFilter = value != 0; break;
            case "buried_treasure_min_fish": buriedTreasureMinFish = value; break;
            case "bastion_enabled": bastionEnabled = value != 0; break;
            case "bastion_allow_bridge": bastionAllowBridge = value != 0; break;
            case "bastion_allow_housing": bastionAllowHousing = value != 0; break;
            case "bastion_allow_stables": bastionAllowStables = value != 0; break;
            case "bastion_allow_treasure": bastionAllowTreasure = value != 0; break;
            case "bastion_max_nether_chunks": bastionMaxNetherChunks = value; break;
            case "fortress_enabled": fortressEnabled = value != 0; break;
            case "fortress_max_nether_chunks": fortressMaxNetherChunks = value; break;
            case "enable_cheats": enableCheats = value != 0; break;
            case "creative_mode": creativeMode = value != 0; break;
            case "thread_count": threadCount = value; break;
            case "or_mode": orMode = value != 0; break;
            default: break;
        }
    }

    public void save(Path path) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            List<String> lines = new ArrayList<>();
            lines.add("village_enabled=" + bit(villageEnabled));
            lines.add("village_max_chunks=" + villageMaxChunks);
            lines.add("ruined_portal_enabled=" + bit(ruinedPortalEnabled));
            lines.add("ruined_portal_max_chunks=" + ruinedPortalMaxChunks);
            lines.add("ruined_portal_looting_sword=" + bit(ruinedPortalLootingSword));
            lines.add("ruined_portal_frame_check=" + bit(ruinedPortalFrameCheck));
            lines.add("buried_treasure_enabled=" + bit(buriedTreasureEnabled));
            lines.add("buried_treasure_max_chunks=" + buriedTreasureMaxChunks);
            lines.add("buried_treasure_min_tnt=" + buriedTreasureMinTnt);
            lines.add("buried_treasure_diamond_filter=" + bit(buriedTreasureDiamondFilter));
            lines.add("buried_treasure_iron_filter=" + bit(buriedTreasureIronFilter));
            lines.add("buried_treasure_min_fish=" + buriedTreasureMinFish);
            lines.add("bastion_enabled=" + bit(bastionEnabled));
            lines.add("bastion_allow_bridge=" + bit(bastionAllowBridge));
            lines.add("bastion_allow_housing=" + bit(bastionAllowHousing));
            lines.add("bastion_allow_stables=" + bit(bastionAllowStables));
            lines.add("bastion_allow_treasure=" + bit(bastionAllowTreasure));
            lines.add("bastion_max_nether_chunks=" + bastionMaxNetherChunks);
            lines.add("fortress_enabled=" + bit(fortressEnabled));
            lines.add("fortress_max_nether_chunks=" + fortressMaxNetherChunks);
            lines.add("enable_cheats=" + bit(enableCheats));
            lines.add("creative_mode=" + bit(creativeMode));
            // thread_count intentionally not written - see the field's own comment.
            lines.add("or_mode=" + bit(orMode));
            Files.write(path, lines);
        } catch (IOException ignored) {
        }
    }

    private static int bit(boolean v) {
        return v ? 1 : 0;
    }
}
