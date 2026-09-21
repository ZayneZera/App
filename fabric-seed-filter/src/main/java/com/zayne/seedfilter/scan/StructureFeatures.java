package com.zayne.seedfilter.scan;

import net.minecraft.world.gen.feature.StructureFeature;

/**
 * StructureFeature constants aren't individually named in this Yarn build (1.16.1+build.21) -
 * looked up by their vanilla string id from the STRUCTURES registry map instead.
 */
public class StructureFeatures {
    public static final StructureFeature<?> VILLAGE = StructureFeature.STRUCTURES.get("Village");
}
