package com.knowyourknot.enderporter;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public final class Lang {
    private Lang() {}

    public static final TranslatableText TELEPORT_NOT_SET = new TranslatableText("enderporter.teleport_not_set");
    public static final MutableText TELEPORT_NOT_SET_GREY = new TranslatableText("enderporter.teleport_not_set").setStyle(Style.EMPTY.withColor(Formatting.GRAY));
    public static final TranslatableText DIFFERENT_DIMENSION = new TranslatableText("enderporter.gui.different_dimension");

    // modders can add localisations for their dimension names
    public static final TranslatableText teleportDimension(DimensionLocation dimLoc) {
        String delocalisedName = "dimension." + dimLoc.getIdentifier().getNamespace() + "." + dimLoc.getIdentifier().getPath();
        return new TranslatableText(delocalisedName);
    }

    public static final TranslatableText teleportLocation(DimensionLocation dimLoc) {
        return new TranslatableText("enderporter.teleport_location", dimLoc.getPosX(), dimLoc.getPosY(), dimLoc.getPosZ());
    }

    public static final TranslatableText blocksAway(int blocks) {
        return new TranslatableText("enderporter.gui.blocks_away", blocks);
    }

    public static final TranslatableText pearlsRequired(int pearls) {
        return new TranslatableText("enderporter.gui.pearls_required", pearls);
    }
}
