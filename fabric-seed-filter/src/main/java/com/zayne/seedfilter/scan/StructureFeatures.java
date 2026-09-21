package com.zayne.seedfilter.scan;

import net.minecraft.world.gen.feature.StructureFeature;

/**
 * StructureFeature constants aren't individually named in this Yarn build (1.16.1+build.21) -
 * looked up by their real vanilla string id, confirmed live from the running game's
 * StructureFeature.STRUCTURES keySet: [pillager_outpost, mineshaft, mansion, jungle_pyramid,
 * desert_pyramid, igloo, ruined_portal, shipwreck, swamp_hut, stronghold, monument, ocean_ruin,
 * fortress, endcity, buried_treasure, village, nether_fossil, bastion_remnant].
 */
public class StructureFeatures {
    public static final StructureFeature<?> VILLAGE = StructureFeature.STRUCTURES.get("village");
    public static final StructureFeature<?> RUINED_PORTAL = StructureFeature.STRUCTURES.get("ruined_portal");
    public static final StructureFeature<?> BURIED_TREASURE = StructureFeature.STRUCTURES.get("buried_treasure");
    public static final StructureFeature<?> BASTION_REMNANT = StructureFeature.STRUCTURES.get("bastion_remnant");
    public static final StructureFeature<?> FORTRESS = StructureFeature.STRUCTURES.get("fortress");
}
