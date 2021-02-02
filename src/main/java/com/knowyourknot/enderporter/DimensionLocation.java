package com.knowyourknot.enderporter;

import net.minecraft.advancement.criterion.Criteria;
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

public class DimensionLocation {
    private static final String DIMENSION_NAMESPACE = "dimensionNamespace";
    private static final String DIMENSION_PATH = "dimensionPath";
    private static final String POS_X = "posX";
    private static final String POS_Y = "posY";
    private static final String POS_Z = "posZ";

    private final String namespace;
    private final String path;
    private final float posX;
    private final float posY;
    private final float posZ;

    public DimensionLocation(String namespace, String path, float posX, float posY, float posZ) {
        this.namespace = namespace;
        this.path = path;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    public boolean isInDimension(Identifier dimensionId) {
        return (dimensionId == this.getIdentifier());
    }

    public float distanceTo(Vec3d pos) {
        return (float) this.getPos().distanceTo(pos);
    }

    public String toString() {
        return this.namespace + ":" + this.path + ", " + this.getPos().toString();
    }

    public static DimensionLocation getContextDimensionLocation(ItemUsageContext context) {
        World world = context.getWorld();
        Identifier dimensionIdentifier = world.getRegistryKey().getValue();
        String namespace = dimensionIdentifier.getNamespace();
        String path = dimensionIdentifier.getPath();

        BlockPos blockPos = context.getBlockPos();
        Direction side = context.getSide();
        // test with whiterabbit https://modrinth.com/mod/WhiteRabbit
        float posX = (float) blockPos.getX() + side.getOffsetX();
        float posY = (float) blockPos.getY() + side.getOffsetY();
        float posZ = (float) blockPos.getZ() + side.getOffsetZ();

        return new DimensionLocation(namespace, path, posX, posY, posZ);
    }

    public static DimensionLocation getStackDimensionLocation(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag data = stack.getTag();
            return DimensionLocation.getNbtDimensionLocation(data);
        } else {
            return null;
        }
    }

    public static DimensionLocation getNbtDimensionLocation(CompoundTag data) {
        if (data.contains(DIMENSION_NAMESPACE) && data.contains(DIMENSION_PATH) && data.contains(POS_X) && data.contains(POS_Y) && data.contains(POS_Z)) {
            String namespace = data.getString(DIMENSION_NAMESPACE);
            String path = data.getString(DIMENSION_PATH);
            float posX = data.getFloat(POS_X);
            float posY = data.getFloat(POS_Y);
            float posZ = data.getFloat(POS_Z);

            return new DimensionLocation(namespace, path, posX, posY, posZ);
        } else {
            return null;
        }
    }

    public void setNbtDimensionLocation(CompoundTag data) {
        data.putString(DIMENSION_NAMESPACE, this.namespace);
        data.putString(DIMENSION_PATH, this.path);
        data.putFloat(POS_X, this.posX);
        data.putFloat(POS_Y, this.posY);
        data.putFloat(POS_Z, this.posZ);
    }

    public static DimensionLocation setContextDimensionLocation(ItemUsageContext context) {
        PlayerEntity playerEntity = context.getPlayer();

        if (playerEntity.isSneaking()) {
            DimensionLocation dimLoc = getContextDimensionLocation(context);

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
        return this.namespace;
    }

    public String getPath() {
        return this.path;
    }

    public float getPosX() {
        return this.posX;
    }

    public float getPosY() {
        return this.posY;
    }

    public float getPosZ() {
        return this.posZ;
    }

    public Vec3d getPos() {
        return new Vec3d(this.posX, this.posY, this.posZ);
    }

    public Identifier getIdentifier() {
        return new Identifier(this.namespace, this.path);
    }

    // TODO can only check if space empty on server, will have to sync this as a packet to server
    public boolean canFitEntity(ServerWorld world, ServerPlayerEntity player) {
        RegistryKey<World> registryKey = RegistryKey.of(Registry.DIMENSION, this.getIdentifier());
        ServerWorld destination = world.getServer().getWorld(registryKey);
        // decide which blocks to check
        float width = player.getWidth();
        float height = player.getHeight();

        int radius = ((int) Math.ceil(width))/2; // next odd integer + 1 / 2
        int diameter = radius * 2 + 1;
        int intHeight = (int) Math.ceil(height);
        // check blocks empty
        BlockPos initialPos = new BlockPos(this.getPos()).add(-radius, 0, -radius);
        for (int y = 0; y < intHeight; y++) {
            for (int x = 0; x < diameter; x++) {
                for (int z = 0; z < diameter; z++) {
                    BlockPos pos = initialPos.add(x, y, z);
                    if (destination.getBlockState(pos).getBlock() != Blocks.AIR) {
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
            serverPlayerEntity.requestTeleport(this.posX + 0.5f, this.posY, this.posZ + 0.5f);
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
        player.refreshPositionAfterTeleport(this.posX + 0.5f, this.posY, this.posZ + 0.5f);
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

}