package com.zayne.seedfilter.scan;

/**
 * Region-grid placement parameters, verified against Cubitect/cubiomes (finders.c) for MC 1.16.1:
 * salt, spacing (region size in chunks), separation (usable offset range in chunks).
 */
public class StructureConfig {
    public final long salt;
    public final int spacing;
    public final int separation;

    public StructureConfig(long salt, int spacing, int separation) {
        this.salt = salt;
        this.spacing = spacing;
        this.separation = separation;
    }

    public static final StructureConfig VILLAGE = new StructureConfig(10387312, 32, 24);
    public static final StructureConfig RUINED_PORTAL = new StructureConfig(34222645, 40, 25);
    public static final StructureConfig BURIED_TREASURE = new StructureConfig(10387320, 1, 1);
    public static final StructureConfig BASTION = new StructureConfig(30084232, 27, 23);
    public static final StructureConfig FORTRESS = new StructureConfig(30084232, 27, 23);
}
