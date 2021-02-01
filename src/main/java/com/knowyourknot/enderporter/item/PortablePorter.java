package com.knowyourknot.enderporter.item;

import net.minecraft.item.ItemStack;

public class PortablePorter extends ItemTeleport {
    public PortablePorter(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
