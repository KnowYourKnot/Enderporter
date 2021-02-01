package com.knowyourknot.enderporter.block;

import java.util.Random;

import com.knowyourknot.enderporter.DimensionLocation;
import com.knowyourknot.enderporter.EnderPorter;
import com.knowyourknot.enderporter.entity.EntityEnderPorter;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlockEnderPorter extends BlockWithEntity {
    private static final int CHARGE_REQUIRED = 20;
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
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity instanceof PlayerEntity) {
            EntityEnderPorter blockEntity = (EntityEnderPorter) world.getBlockEntity(pos);
            PlayerEntity player = (PlayerEntity) entity;
            if (player.isInSneakingPose()) {
                this.playerCharge(world, blockEntity, player);
            } else {
                // blockEntity.removePlayerFromCharger(player);
            }
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    public void playerCharge(World world, EntityEnderPorter blockEntity, PlayerEntity player) {
        // blockEntity.addPlayerToCharger(player);
        int charge = 0; //blockEntity.getPlayerCharge(player);
        EnderPorter.LOGGER.info(charge);
        if (charge >= CHARGE_REQUIRED) {
            // play sound before teleport
            DimensionLocation dimLoc = blockEntity.getDimensionLocation();
            EnderPorter.LOGGER.info(dimLoc);
            if (dimLoc != null && blockEntity.hasPearlsRequired()) {
                player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
                // sync this on server and client
                // blockEntity.removePlayerFromCharger(player);
                if ((world instanceof ServerWorld)) {
                    dimLoc.moveEntity(world, (ServerPlayerEntity) player);
                    blockEntity.onTeleport();
                }
                //particles after teleport
                for (int i = 0; i < PARTICLE_NUMBER; i++) {
                    world.addParticle(ParticleTypes.PORTAL, (double)dimLoc.getPosX() + 0.5D, (double)dimLoc.getPosY() + player.getHeight() * EnderPorter.RANDOM.nextDouble() - 0.25D, (double)dimLoc.getPosZ() + 0.5D, (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D, -EnderPorter.RANDOM.nextDouble(), (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D);
                }
            } else {
                player.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0F, 1.0F);
                // blockEntity.removePlayerFromCharger(player);
            }
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        EntityEnderPorter blockEntity = (EntityEnderPorter) world.getBlockEntity(pos);
        if (blockEntity.getDimensionLocation() != null) {
            for (int i = 0; i < random.nextInt(3) + 1; ++i) {
                world.addParticle(ParticleTypes.PORTAL, (double) pos.getX() + 0.5D, (double) pos.getY() + 1,
                        (double) pos.getZ() + 0.5D, (random.nextDouble() - 0.5D) * 2.0D, -random.nextDouble(), (random.nextDouble() - 0.5D) * 2.0D);
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
