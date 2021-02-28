package com.knowyourknot.enderporter.item;

import java.util.List;

import com.knowyourknot.enderporter.DimPos;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ItemTeleport extends Item {
    public static final int CHARGE_REQUIRED = 25;
    public static final int MAX_USE_TIME = 72000;
    public static final int PARTICLE_NUMBER = 20;

    public static final String TRAVEL_LIMIT = "travel_limit";
    public static final String ALLOW_TELEPORT_TO_VOID = "allow_teleport_to_void";
    public static final String ALLOW_INTERDIMENSIONAL_TRAVEL = "allow_interdimensional_travel";

    public ItemTeleport(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext tooltipContext) {
        DimPos dimLoc = DimPos.getStackDimPos(stack);
        if (dimLoc != null) {
            tooltip.add(Lang.teleportDimension(dimLoc).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
            tooltip.add(Lang.teleportLocation(dimLoc).setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        } else {
            MutableText teleportNotSet = new TranslatableText(Lang.GUI_TELEPORT_NOT_SET)
                    .setStyle(Style.EMPTY.withColor(Formatting.GRAY));
            tooltip.add(teleportNotSet);
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
            DimPos dimLoc = DimPos.setContextDimensionLocation(context);
            if (dimLoc != null) {
                // we only want this sound to play on the specified player's client
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
        if (user instanceof PlayerEntity && !world.isClient()) {
            if (MAX_USE_TIME - remainingUseTicks >= CHARGE_REQUIRED) {
                tryTeleportPlayer((ServerWorld) world, (ServerPlayerEntity) user, Hand.MAIN_HAND);
            } else {
                world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS,
                        1.0F, 1.0F);
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

    public void tryTeleportPlayer(ServerWorld world, ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        DimPos dimLoc = DimPos.getStackDimPos(stack);
        if (world.isClient()) {
            return;
        }
        if (dimLoc == null) {
            this.onFailedTeleport(world, player);
            MutableText text = new TranslatableText(Lang.MESSAGE_TELEPORT_NOT_SET)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (dimLoc.isInVoid(world) && !EnderPorter.getConfigBool(ALLOW_TELEPORT_TO_VOID)) {
            this.onFailedTeleport(world, player);
            MutableText text = new TranslatableText(Lang.MESSAGE_VOID).setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        int travelLimit = EnderPorter.getConfigInt(TRAVEL_LIMIT);
        if (dimLoc.isInDimension(world.getRegistryKey().getValue()) && travelLimit >= 0 && travelLimit < dimLoc.distanceTo(player.getBlockPos())) {
            this.onFailedTeleport(world, player);
            MutableText text = Lang.messageTravelLimit(travelLimit)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (!dimLoc.isInDimension(world.getRegistryKey().getValue()) && !EnderPorter.getConfigBool(ALLOW_INTERDIMENSIONAL_TRAVEL)) {
            this.onFailedTeleport(world, player);
            MutableText text = new TranslatableText(Lang.MESSAGE_NO_INTERDIMENSIONAL_TRAVEL)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        if (!dimLoc.canFitEntity(world, player)) {
            this.onFailedTeleport(world, player);
            MutableText text = new TranslatableText(Lang.MESSAGE_TELEPORT_LOCATION_OBSTRUCTED)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED));
            player.sendMessage(text, true);
            return;
        }
        BlockPos pos = player.getBlockPos();
        // particles and sound at initial pos
        for (int i = 0; i < 25; i++) {
            Vec3d particlePos = new Vec3d((double) pos.getX() + 0.5D, player.getRandomBodyY() - 0.25D,
                    (double) pos.getZ() + 0.5D);
            Vec3d particleVel = new Vec3d((EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D,
                    -EnderPorter.RANDOM.nextDouble(), (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D);
            world.spawnParticles(ParticleTypes.PORTAL, particlePos.getX(), particlePos.getY(), particlePos.getZ(), 1,
                    particleVel.getX(), particleVel.getY(), particleVel.getZ(), particleVel.length());
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F,
                1.0F);

        dimLoc.moveEntity(world, player);
        pos = player.getBlockPos();
        // particles and sound at new pos
        for (int i = 0; i < 25; i++) {
            world.spawnParticles(ParticleTypes.PORTAL, (double) pos.getX() + 0.5D, player.getRandomBodyY() - 0.25D,
                    (double) pos.getZ() + 0.5D, 1, (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D,
                    -EnderPorter.RANDOM.nextDouble(), (EnderPorter.RANDOM.nextDouble() - 0.5D) * 2.0D, 1);
        }
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F,
                1.0F);
        this.afterTeleport(world, player, hand);
    }

    public void onFailedTeleport(World world, PlayerEntity player) {
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0F,
                1.0F);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (MAX_USE_TIME - user.getItemUseTimeLeft() >= CHARGE_REQUIRED) {
            user.stopUsingItem();
        }
    }
}
