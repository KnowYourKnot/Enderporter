package com.knowyourknot.enderporter;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.DifficultyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.source.BiomeAccess;

public class DimPos {
    private static final String DIMENSION_NAMESPACE = "dimensionNamespace";
    private static final String DIMENSION_PATH = "dimensionPath";
    private static final String POS_X = "posX";
    private static final String POS_Y = "posY";
    private static final String POS_Z = "posZ";

    private final Identifier identifier;
    private final BlockPos pos;

    public DimPos(Identifier identifier, BlockPos pos) {
        this.identifier = identifier;
        this.pos = pos;
    }

    public boolean isInDimension(Identifier dimensionId) {
        return (this.getIdentifier().equals(dimensionId));
    }

    public float distanceTo(BlockPos target) {
        Vec3d startPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        return (float) startPos.distanceTo(targetPos);
    }

    public String toString() {
        return identifier.toString() + ", " + pos.toString();
    }

    public static DimPos getContextDimPos(ItemUsageContext context) {
        World world = context.getWorld();
        Identifier newIdentifier = world.getRegistryKey().getValue();

        BlockPos newPos = context.getBlockPos();
        Direction side = context.getSide();
        // TODO test with whiterabbit https://modrinth.com/mod/WhiteRabbit
        // the offset when setting the location to the underside of a block aren't quite right
        // there is no way to fix this without getting the size of the entity making the dimpos
        // and I want the dimpos to be independent of the entity.
        return new DimPos(newIdentifier, newPos.add(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ()));
    }

    public static DimPos getStackDimPos(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag data = stack.getTag();
            return DimPos.getNbtDimPos(data);
        } else {
            return null;
        }
    }

    public static DimPos getNbtDimPos(CompoundTag data) {
        if (data.contains(DIMENSION_NAMESPACE) && data.contains(DIMENSION_PATH) && data.contains(POS_X) && data.contains(POS_Y) && data.contains(POS_Z)) {
            String namespace = data.getString(DIMENSION_NAMESPACE);
            String path = data.getString(DIMENSION_PATH);
            Identifier newIdentifier = new Identifier(namespace, path);
            BlockPos newPos = new BlockPos(data.getFloat(POS_X), data.getFloat(POS_Y), data.getFloat(POS_Z));
            return new DimPos(newIdentifier, newPos);
        } else {
            return null;
        }
    }

    public void setNbtDimensionLocation(CompoundTag data) {
        data.putString(DIMENSION_NAMESPACE, this.getNamespace());
        data.putString(DIMENSION_PATH, this.getPath());
        data.putFloat(POS_X, this.pos.getX());
        data.putFloat(POS_Y, this.pos.getY());
        data.putFloat(POS_Z, this.pos.getZ());
    }

    public static DimPos setContextDimensionLocation(ItemUsageContext context) {
        PlayerEntity playerEntity = context.getPlayer();

        if (playerEntity.isSneaking()) {
            DimPos dimLoc = getContextDimPos(context);

            ItemStack stack = context.getStack();
            CompoundTag data;

            if (stack.hasTag()) {
                data = stack.getTag();
            } else {
                data = new CompoundTag();
            }

            dimLoc.setNbtDimensionLocation(data);
            stack.setTag(data);
            return dimLoc;
        }
        return null;
    }

    public String getNamespace() {
        return this.identifier.getNamespace();
    }

    public String getPath() {
        return this.identifier.getPath();
    }

    public BlockPos getPos() {
        return pos.mutableCopy();
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public boolean canFitEntity(ServerWorld world, ServerPlayerEntity player) {
        RegistryKey<World> registryKey = RegistryKey.of(Registry.DIMENSION, this.identifier);
        ServerWorld destination = world.getServer().getWorld(registryKey);
        // decide which blocks to check
        float width = player.getWidth();
        float height = player.getHeight();

        int radius = ((int) Math.ceil(width))/2; // next odd integer + 1 / 2
        int diameter = radius * 2 + 1;
        int intHeight = (int) Math.ceil(height);
        // check blocks empty
        BlockPos initialPos = new BlockPos(this.pos).add(-radius, 0, -radius);
        for (int y = 0; y < intHeight; y++) {
            for (int x = 0; x < diameter; x++) {
                for (int z = 0; z < diameter; z++) {
                    BlockPos posToCheck = initialPos.add(x, y, z);
                    Block blockAtPos = destination.getBlockState(posToCheck).getBlock();
                    if (blockAtPos != Blocks.AIR && blockAtPos != Blocks.CAVE_AIR && blockAtPos != Blocks.VOID_AIR) {
                        EnderPorter.LOGGER.info(posToCheck);
                        EnderPorter.LOGGER.info(destination.getBlockState(posToCheck).getBlock());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public ServerPlayerEntity moveEntity(World world, ServerPlayerEntity serverPlayerEntity) {
        RegistryKey<World> registryKey = RegistryKey.of(Registry.DIMENSION, this.getIdentifier());
        ServerWorld destination = ((ServerWorld) world).getServer().getWorld(registryKey);
        ServerWorld currentWorld = (ServerWorld) serverPlayerEntity.world;
        
        if (currentWorld.getRegistryKey() != destination.getRegistryKey()) {
            this.moveToDimension(destination, serverPlayerEntity);
        } else {
            serverPlayerEntity.requestTeleport(this.pos.getX() + 0.5f, this.pos.getY(), this.pos.getZ() + 0.5f);
        }

        return serverPlayerEntity;

    }

    // from ServerPlayerEntity.moveToWorld, inspired by kryptonaut's custom portal API
    private ServerPlayerEntity moveToDimension(ServerWorld destination, ServerPlayerEntity player) {
        ServerWorld origin = player.getServerWorld();
        WorldProperties worldProperties = destination.getLevelProperties();
        player.networkHandler.sendPacket(new PlayerRespawnS2CPacket(destination.getDimension(), destination.getRegistryKey(), BiomeAccess.hashSeed(destination.getSeed()), player.interactionManager.getGameMode(), player.interactionManager.getPreviousGameMode(), destination.isDebugWorld(), destination.isFlat(), true));
        player.networkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
        PlayerManager playerManager = player.server.getPlayerManager();
        playerManager.sendCommandTree(player);
        origin.removePlayer(player);
        player.removed = false;

        origin.getProfiler().pop();
        origin.getProfiler().push("placing");
        player.setWorld(destination);
        destination.onPlayerChangeDimension(player);
        player.refreshPositionAfterTeleport(this.pos.getX() + 0.5f, this.pos.getY(), this.pos.getZ() + 0.5f);
        origin.getProfiler().pop();
        worldChanged(origin, player);
        player.interactionManager.setWorld(destination);
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.abilities));
        playerManager.sendWorldInfo(player, destination);
        playerManager.sendPlayerStatus(player);

        for (StatusEffectInstance statusEffectInstance : player.getStatusEffects()) {
            player.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getEntityId(), statusEffectInstance));
        }

        player.networkHandler.sendPacket(new WorldEventS2CPacket(1032, BlockPos.ORIGIN, 0, false));
        // moveToWorld then tells the game that health, hunger and xp are synced, but this doesn't appear to be necessary
        return player;
    }

    private static void worldChanged(ServerWorld origin, ServerPlayerEntity player) {
        RegistryKey<World> registryKey = origin.getRegistryKey();
        RegistryKey<World> registryKey2 = player.world.getRegistryKey();
        Criteria.CHANGED_DIMENSION.trigger(player, registryKey, registryKey2);
    }

    public boolean isInVoid() {
        return this.pos.getY() < 0 || this.pos.getY() > 255;
    }
}