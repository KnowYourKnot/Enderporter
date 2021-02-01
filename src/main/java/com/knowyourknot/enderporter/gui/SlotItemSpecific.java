package com.knowyourknot.enderporter.gui;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class SlotItemSpecific extends Slot {
    private final Item[] validItems;

    public SlotItemSpecific(Inventory inventory, int index, int x, int y, Item[] validItems) {
        super(inventory, index, x, y);
        this.validItems = validItems;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        Item stackItem = stack.getItem();
        for (int i = 0; i < this.validItems.length; i++) {
            if (stackItem == this.validItems[i]) {
                return true;
            }
        }
        return false;
    }
    
}
