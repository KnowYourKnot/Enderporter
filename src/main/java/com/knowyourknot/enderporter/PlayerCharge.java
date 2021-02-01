package com.knowyourknot.enderporter;

import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;

public class PlayerCharge {
    private UUID uuid;
    private String name;
    private int charge;

    public PlayerCharge(PlayerEntity player) {
        this.uuid = player.getUuid();
        this.name = player.getGameProfile().getName();
        this.charge = 0;
    }

    public PlayerCharge(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.charge = 0;
    }

    public static PlayerCharge fromTag(CompoundTag tag) {
        UUID uuid = tag.getUuid("UUID");
        String name = tag.getString("Name");
        PlayerCharge playerCharge = new PlayerCharge(uuid, name);
        playerCharge.setCharge(tag.getInt("Charge"));
        return playerCharge;
    }

    public void toTag(CompoundTag tag) {
        tag.putUuid("UUID", this.uuid);
        tag.putString("Name", this.name);
        tag.putInt("Charge", this.charge);
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    public int getCharge() {
        return this.charge;
    }

    public void incrementCharge(int increment) {
        this.setCharge(this.getCharge() + increment);
    }

    public boolean isPlayer(PlayerEntity player) {
        UUID newUuid = player.getUuid();
        String newName = player.getGameProfile().getName();
        return (uuid == newUuid || name.equals(newName));
    }
}