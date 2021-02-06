package com.knowyourknot.enderporter.gui;

import com.knowyourknot.enderporter.EnderPorter;
import com.knowyourknot.enderporter.item.ItemTeleport;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class EnderPorterScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private boolean freeTravelAllowed;

    PropertyDelegate propertyDelegate;

    public EnderPorterScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, new SimpleInventory(12), new ArrayPropertyDelegate(4));
        freeTravelAllowed = buf.readBoolean();
    }

    public EnderPorterScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
            PropertyDelegate propertyDelegate) {
        super(EnderPorter.ENDER_PORTER_SCREEN_HANDLER, syncId);
        checkSize(inventory, 12);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.freeTravelAllowed = EnderPorter.getConfigBool("allow_free_travel");
        inventory.onOpen(playerInventory.player);
        this.addProperties(propertyDelegate);

        Item[] validTeleportItems = { EnderPorter.STABLE_PEARL, EnderPorter.PORTABLE_PORTER };
        this.addSlot(new SlotItemSpecific(inventory, 0, 12, 45, validTeleportItems)); // tp item slot

        // inv
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 3; y++) {
                this.addSlot(new Slot(playerInventory, 9 + x + 9 * y, 8 + 18 * x, 123 + 18 * y));
            }
        }

        // hotbar
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInventory, x, 8 + 18 * x, 181));
        }

        if (!this.freeTravelAllowed()) {
            Item[] upgradeRange = { EnderPorter.UPGRADE_RANGE };
            this.addSlot(new SlotItemSpecific(inventory, 1, 8, 19, upgradeRange)); // range slot
            Item[] upgradeDim = { EnderPorter.UPGRADE_DIM };
            this.addSlot(new SlotItemSpecific(inventory, 2, 8, 71, upgradeDim)); // dim slot

            // ender
            Item[] enderPearl = { Items.ENDER_PEARL };
            for (int x = 0; x < 9; x++) {
                this.addSlot(new SlotItemSpecific(inventory, 3 + x, 8 + 18 * x, 97, enderPearl));
            }            
        }

    }

    public int getBlocksAway() {
        return propertyDelegate.get(0);
    }

    public int getPearlsRequired() {
        return propertyDelegate.get(1);
    }

    public boolean hasPearlsRequired() {
        return propertyDelegate.get(2) == 1;
    }

    public boolean freeTravelAllowed() {
        return freeTravelAllowed;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            if (freeTravelAllowed() && !(originalStack.getItem() instanceof ItemTeleport)) {
                return ItemStack.EMPTY;
            }
            newStack = originalStack.copy();
            if (invSlot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

}
