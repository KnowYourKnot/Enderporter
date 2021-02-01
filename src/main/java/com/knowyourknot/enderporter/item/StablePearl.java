package com.knowyourknot.enderporter.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class StablePearl extends ItemTeleport {
    public StablePearl(Settings settings) {
        super(settings);
    }

    @Override
    public void afterTeleport(World world, PlayerEntity playerEntity, Hand hand) {
        PlayerInventory inventory = playerEntity.inventory;
        inventory.removeStack(inventory.selectedSlot);
    }
}
