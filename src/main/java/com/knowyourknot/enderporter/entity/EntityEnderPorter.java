package com.knowyourknot.enderporter.entity;

import com.knowyourknot.enderporter.DimPos;
import com.knowyourknot.enderporter.EnderPorter;
import com.knowyourknot.enderporter.Lang;
import com.knowyourknot.enderporter.PlayerCharger;
import com.knowyourknot.enderporter.gui.EnderPorterScreenHandler;
import com.knowyourknot.enderporter.inventory.IInventory;
import com.knowyourknot.enderporter.item.ItemTeleport;

import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;

public class EntityEnderPorter extends BlockEntity
        implements Tickable, IInventory, ExtendedScreenHandlerFactory, BlockEntityClientSerializable, SidedInventory {
    private static final String ALLOW_FREE_TRAVEL = "allow_free_travel";
    private static final String BLOCKS_PER_PEARL = "blocks_per_pearl";

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(12, ItemStack.EMPTY);
    private final PlayerCharger playerCharger = new PlayerCharger();

    private boolean allowFreeTravel = false;

    public EntityEnderPorter() {
        super(EnderPorter.ENTITY_ENDER_PORTER);
    }

    public DimPos getDimensionLocation() {
        return DimPos.getStackDimPos(this.items.get(0));
    }

    public int getBlocksAway() {
        DimPos dimLoc = this.getDimensionLocation();
        Identifier currentWorldIdentifier = this.getWorld().getRegistryKey().getValue();

        if (dimLoc != null && dimLoc.getIdentifier().compareTo(currentWorldIdentifier) == 0) {
            return (int) dimLoc.distanceTo(this.getPos());
        }
        return -1;
    }

    public void onTeleport() {
        // use pearls
        int pearlsRequired = getPearlsRequired();
        for (int i = 11; i >= 0; i--) {
            ItemStack itemsRemoved = removeStack(i, pearlsRequired);
            pearlsRequired -= itemsRemoved.getCount();
            if (pearlsRequired == 0) {
                return;
            }
        }
    }

    public boolean needsDimUpgrade() {
        Identifier dimensionId = this.getWorld().getRegistryKey().getValue();
        return !this.getDimensionLocation().isInDimension(dimensionId);
    }

    // if inv contains dimensional upgrade
    public boolean hasDimUpgrade() {
        return (this.items.get(2) != ItemStack.EMPTY);
    }

    // number of range upgrades contained in inv
    public int getRangeUpgrades() {
        ItemStack stack = this.items.get(1);
        if (stack != ItemStack.EMPTY) {
            return this.items.get(1).getCount();
        }
        return 0;
    }

    // number of pearls inv contains
    public int getPearls() {
        int pearlSum = 0;
        for (int i = 3; i < 12; i++) {
            ItemStack stack = this.items.get(i);
            if (stack != ItemStack.EMPTY) {
                pearlSum += stack.getCount();
            }
        }
        return pearlSum;
    }

    // number of blocks one pearl will transport the player
    public int getBlocksPerPearl() {
        if (hasDimUpgrade()) {
            return 0;
        } else {
            return EnderPorter.getConfigInt(BLOCKS_PER_PEARL) * (int) Math.pow(2, getRangeUpgrades());
        }
    }

    // number of pearls player needs to tp to current dimLoc
    public int getPearlsRequired() {
        int blocksPerPearl = getBlocksPerPearl();
        if (EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL)) {
            return 0;
        }
        if (blocksPerPearl > 0) {
            return (getBlocksAway() / blocksPerPearl);
        }
        return 0;
    }

    public boolean hasPearlsRequired() {
        DimPos dimLoc = this.getDimensionLocation();
        if (dimLoc == null) {
            return false;
        }
        Identifier currentDimension = this.getWorld().getRegistryKey().getValue();
        if (dimLoc.getIdentifier().compareTo(currentDimension) == 0) {
            return (getPearls() >= getPearlsRequired());
        } else {
            return hasDimUpgrade();
        }
    }

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            if (index == 0) {
                return getBlocksAway();
            } else if (index == 1) {
                return getPearlsRequired();
            } else if (index == 2) {
                return hasPearlsRequired() ? 1 : 0;
            } else {
                return freeTravelAllowed() ? 1 : 0;
            }
        }

        @Override
        public void set(int index, int value) {
            assert true;
        }

        @Override
        public int size() {
            return 4;
        }
    };

    public boolean freeTravelAllowed() {
        if (world.isClient) {
            return allowFreeTravel;
        }
        return EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL);
    }

    // this seems to be called twice when an item is added to the inventory, but
    // only once when an item is removed
    @Override
    public void markDirty() {
        super.markDirty();
        if (!this.world.isClient) {
            sync();
        }
    }

    @Override
    public ItemStack removeStack(int slot, int count) {
        ItemStack result = Inventories.splitStack(getItems(), slot, count);
        this.markDirty();
        return result;
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        markDirty();
        return new EnderPorterScreenHandler(syncId, playerInventory, this, propertyDelegate);
    }

    @Override
    public Text getDisplayName() {
        return new TranslatableText(Lang.GUI_ENDER_PORTER);
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    // these conditions are present elsewhere in the code, are they necessary here?
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == 0) {
            return stack.getItem() instanceof ItemTeleport;
        } else if (EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL)) {
            return false;
        } else if (slot == 1) {
            return stack.getItem() == EnderPorter.UPGRADE_RANGE;
        } else if (slot == 2) {
            return stack.getItem() == EnderPorter.UPGRADE_DIM;
        } else if (slot < 12) {
            return stack.getItem() == Items.ENDER_PEARL;
        }
        return false;
    }

    // read
    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        Inventories.fromTag(tag, this.items);
    }

    // write
    @Override
    public CompoundTag toTag(CompoundTag tag) {
        super.toTag(tag);
        Inventories.toTag(tag, this.items);
        return tag;
    }

    // read on client
    @Override
    public void fromClientTag(CompoundTag tag) {
        this.items.clear(); // fromTag does not remove non-present items as it assumes the inventory it is
                            // filling is empty
        Inventories.fromTag(tag, this.items);
        allowFreeTravel = (tag.getInt("easy_mode") == 1);
    }

    // write on server
    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        Inventories.toTag(tag, this.items);
        tag.putInt("easy_mode", EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL) ? 1 : 0);
        return tag;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL)) {
            return new int[0];
        }
        int[] availableSlots = new int[9];
        for (int i = 0; i < 9; i++) {
            availableSlots[i] = i + 3;
        }
        return availableSlots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return !EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL) && dir != Direction.UP;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return !EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL) && dir != Direction.UP;
    }

    @Override
    public void tick() {
        if (!this.world.isClient() && !this.playerCharger.isEmpty()) {
            this.playerCharger.incrementAll(1);
            this.markDirty();
        }
    }

    // returns false if player already in charger
    public boolean addPlayerToCharger(PlayerEntity player) {
        if (!this.playerCharger.isPlayerCharging(player)) {
            this.playerCharger.addPlayer(player);
            return true;
        }
        return false;
    }

    public boolean removePlayerFromCharger(PlayerEntity player) {
        return this.playerCharger.removePlayer(player);
    }

    public int getPlayerCharge(PlayerEntity player) {
        return this.playerCharger.getCharge(player);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBoolean(EnderPorter.getConfigBool(ALLOW_FREE_TRAVEL));
    }
}
