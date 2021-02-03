package com.knowyourknot.enderporter;

import net.minecraft.text.TranslatableText;

public final class Lang {
    private Lang() {}

    public static final String GUI_ENDER_PORTER = "gui.enderporter.ender_porter";
    public static final String GUI_TELEPORT_LOCATION = "gui.enderporter.teleport_location";
    public static final String GUI_TELEPORT_NOT_SET = "gui.enderporter.teleport_not_set";
    public static final String GUI_BLOCKS_AWAY = "gui.enderporter.blocks_away";
    public static final String GUI_DIFFERENT_DIMENSION = "gui.enderporter.different_dimension";
    public static final String GUI_PEARLS_REQUIRED = "gui.enderporter.pearls_required";

    public static final String MESSAGE_TELEPORT_LOCATION_OBSTRUCTED = "message.enderporter.teleport_location_obstructed";
    public static final String MESSAGE_TELEPORT_NOT_SET = "message.enderporter.teleport_not_set";
    public static final String MESSAGE_NOT_ENOUGH_PEARLS = "message.enderporter.not_enough_pearls";
    public static final String MESSAGE_INTERDIMENSIONAL_UPGRADE_MISSING = "message.enderporter.interdimensional_upgrade_missing";

    // modders can add localisations for their dimension names
    public static final TranslatableText teleportDimension(DimPos dimLoc) {
        String delocalisedName = "dimension." + dimLoc.getIdentifier().getNamespace() + "." + dimLoc.getIdentifier().getPath();
        return new TranslatableText(delocalisedName);
    }

    public static final TranslatableText teleportLocation(DimPos dimLoc) {
        return new TranslatableText(GUI_TELEPORT_LOCATION, dimLoc.getPos().getX(), dimLoc.getPos().getY(), dimLoc.getPos().getZ());
    }

    public static final TranslatableText blocksAway(int blocks) {
        return new TranslatableText(GUI_BLOCKS_AWAY, blocks);
    }

    public static final TranslatableText pearlsRequired(int pearls) {
        return new TranslatableText(GUI_PEARLS_REQUIRED, pearls);
    }
}
