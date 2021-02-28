package com.knowyourknot.enderporter.block;

import java.util.Random;

import com.knowyourknot.enderporter.DimPos;
import com.knowyourknot.enderporter.EnderPorter;
import com.knowyourknot.enderporter.Lang;
import com.knowyourknot.enderporter.entity.EntityEnderPorter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockEnderPorter extends BlockWithEntity {
    private static final String CHARGE_REQUIRED = "porter_charge_time";
    public static final String TRAVEL_LIMIT = "travel_limit";
    public static final String ALLOW_TELEPORT_TO_VOID = "allow_teleport_to_void";
    public static final String ALLOW_INTERDIMENSIONAL_TRAVEL = "allow_interdimensional_travel";
    public static final int PARTICLE_NUMBER = 20;

    public BlockEnderPorter(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView blockView) {
        return new EntityEnderPorter();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState blockState, World world, BlockPos blockPos, PlayerEntity player, Hand hand,
            BlockHitResult blockHitResult) {
        if (!world.isClient()) {
            NamedScreenHandlerFactory screenHandlerFactory = blockState.createScreenHandlerFactory(world, blockPos);
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof EntityEnderPorter) {
                ItemScatterer.spawn(world, pos, (EntityEnderPorter) entity);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity instanceof PlayerEntity) {
            EntityEnderPorter blockEntity = (EntityEnderPorter) world.getBlockEntity(pos);
            if (!world.isClient && entity.isInSneakingPose()) {
                this.playerCharge((ServerWorld) world, pos, blockEntity, (ServerPlayerEntity) entity);
            } else {
                blockEntity.removePlayerFromCharger((PlayerEntity) entity);
            }
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    public void playerCharge(ServerWorld world, BlockPos pos, EntityEnderPorter blockEntity,
            ServerPlayerEntity player) {
        blockEntity.addPlayerToCharger(player);
        int charge = blockEntity.getPlayerCharge(player);
        if (charge < EnderPorter.getConfigInt(CHARGE_REQUIRED)) {
            return;
        }
        DimPos dimLoc = blockEntity.getDimensionLocation();
        if (dimLoc == null) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = new TranslatableText(Lang.MESSAGE_TELEPORT_NOT_SET)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (dimLoc.isInVoid(world) && !EnderPorter.getConfigBool(ALLOW_TELEPORT_TO_VOID)) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = new TranslatableText(Lang.MESSAGE_VOID).setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        int travelLimit = EnderPorter.getConfigInt(TRAVEL_LIMIT);
        if (!blockEntity.needsDimUpgrade() && travelLimit >= 0 && travelLimit < blockEntity.getBlocksAway()) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = Lang.messageTravelLimit(travelLimit)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (blockEntity.needsDimUpgrade() && !EnderPorter.getConfigBool(ALLOW_INTERDIMENSIONAL_TRAVEL)) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = new TranslatableText(Lang.MESSAGE_NO_INTERDIMENSIONAL_TRAVEL)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (blockEntity.needsDimUpgrade() && !blockEntity.hasDimUpgrade()) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = new TranslatableText(Lang.MESSAGE_INTERDIMENSIONAL_UPGRADE_MISSING)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (!blockEntity.hasPearlsRequired()) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = new TranslatableText(Lang.MESSAGE_NOT_ENOUGH_PEARLS)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (!dimLoc.canFitEntity(world, player)) {
            this.onFailedTeleport(world, pos, player, blockEntity);
            MutableText text = new TranslatableText(Lang.MESSAGE_TELEPORT_LOCATION_OBSTRUCTED)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        blockEntity.removePlayerFromCharger(player);
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F, 1.0F);
        player.setPose(EntityPose.STANDING);
        dimLoc.moveEntity(world, player);
        blockEntity.onTeleport();
        // particles and sound after teleport
        world.playSound(null, new BlockPos(dimLoc.getPos()), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS,
                1.0F, 1.0F);
        for (int i = 0; i < PARTICLE_NUMBER; i++) {
            BlockPos particlePos = dimLoc.getPos().add(0.5D,
                    player.getHeight() * EnderPorter.RANDOM.nextDouble() - 0.25D, 0.5D);
            Vec3d particleVel = new Vec3d((EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D,
                    -EnderPorter.RANDOM.nextDouble(), (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D);
            world.spawnParticles(ParticleTypes.PORTAL, particlePos.getX(), particlePos.getY(), particlePos.getZ(), 1,
                    particleVel.getX(), particleVel.getY(), particleVel.getZ(), particleVel.length());
        }
    }

    public void onFailedTeleport(World world, BlockPos pos, PlayerEntity player, EntityEnderPorter blockEntity) {
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        blockEntity.removePlayerFromCharger(player);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        EntityEnderPorter blockEntity = (EntityEnderPorter) world.getBlockEntity(pos);
        if (blockEntity.getDimensionLocation() != null) {
            for (int i = 0; i < random.nextInt(3) + 1; ++i) {
                world.addParticle(ParticleTypes.PORTAL, (double) pos.getX() + 0.5D, (double) pos.getY() + 1,
                        (double) pos.getZ() + 0.5D, (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(),
                        (random.nextDouble() - 0.5D) * 2.0D);
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0f, 0f, 0f, 1f, 0.5f, 1f);
    }

    @Override
    public boolean hasSidedTransparency(BlockState state) {
        return true;
    }
}
