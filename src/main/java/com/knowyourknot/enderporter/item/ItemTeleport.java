package com.knowyourknot.enderporter.item;

import java.util.List;

import com.knowyourknot.enderporter.DimensionLocation;
import com.knowyourknot.enderporter.EnderPorter;
import com.knowyourknot.enderporter.Lang;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemTeleport extends Item {
    public static final int CHARGE_REQUIRED = 25;
    public static final int MAX_USE_TIME = 72000;
    public static final int PARTICLE_NUMBER = 20;
    
    public ItemTeleport(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        DimensionLocation dimLoc = DimensionLocation.getStackDimensionLocation(stack);
        if (dimLoc != null) {
            tooltip.add(Lang.teleportDimension(dimLoc).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            tooltip.add(Lang.teleportLocation(dimLoc).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        } else {
            tooltip.add(Lang.TELEPORT_NOT_SET_GREY);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        playerEntity.setCurrentHand(hand);
        return TypedActionResult.pass(playerEntity.getStackInHand(hand));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity playerEntity = context.getPlayer();
        if (playerEntity.isSneaking()) {
            DimensionLocation dimLoc = DimensionLocation.setContextDimensionLocation(context);
            if (dimLoc != null) {
                context.getPlayer().playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                return ActionResult.SUCCESS;
            }
        } else {
            playerEntity.setCurrentHand(context.getHand());
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity) {
            if (MAX_USE_TIME - remainingUseTicks >= CHARGE_REQUIRED) {
                tryTeleportPlayer(world, (PlayerEntity) user, Hand.MAIN_HAND);
            } else {
                ((PlayerEntity) user).playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0F, 1.0F);
            }
        }
    }

    public void afterTeleport(World world, PlayerEntity player, Hand hand) {
        // triggers on successful teleport
        // here in case any child items want to use it (stable pearl)
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return MAX_USE_TIME;
    }

    public void tryTeleportPlayer(World world, PlayerEntity playerEntity, Hand hand) {
        ItemStack stack = playerEntity.getStackInHand(hand);
        DimensionLocation dimLoc = DimensionLocation.getStackDimensionLocation(stack);
        if (dimLoc != null) {
            BlockPos pos = playerEntity.getBlockPos();
            // particles and sound at old location
            playerEntity.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            for (int i = 0; i < 25; i++) {
                world.addParticle(ParticleTypes.PORTAL, (double)pos.getX() + 0.5D, playerEntity.getRandomBodyY() - 0.25D, (double)pos.getZ() + 0.5D, (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D, -EnderPorter.RANDOM.nextDouble(), (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D);
            }
            if (world instanceof ServerWorld) {
                dimLoc.moveEntity(world, (ServerPlayerEntity)playerEntity);
            }
            // particles and sound at new location
            playerEntity.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            for (int i = 0; i < PARTICLE_NUMBER; i++) {
                world.addParticle(ParticleTypes.PORTAL, (double)dimLoc.getPosX() + 0.5D, (double)dimLoc.getPosY() + playerEntity.getHeight() * EnderPorter.RANDOM.nextDouble() - 0.25D, (double)dimLoc.getPosZ() + 0.5D, (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D, -EnderPorter.RANDOM.nextDouble(), (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D);
            }
            this.afterTeleport(world, playerEntity, hand);
        } else {
            playerEntity.playSound(SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0F, 1.0F);
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (MAX_USE_TIME - user.getItemUseTimeLeft() >= CHARGE_REQUIRED) {
            user.stopUsingItem();
        }
    }
}
