package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.lib.placement.BlockPlacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class AutoParkourHandler {

    private long lastPlacementTime = 0;
    private boolean wasKeyDown = false;

    public void onTick(Minecraft client) {
        if (InputController.autoParkourKey == null) return;
        
        // Toggle logic
        boolean isKeyDown = InputController.autoParkourKey.isDown();
        if (isKeyDown && !wasKeyDown) {
            is.pig.minecraft.build.config.PiggyBuildConfig config = is.pig.minecraft.build.config.PiggyBuildConfig.getInstance();
            config.setAutoParkourEnabled(!config.isAutoParkourEnabled());
            is.pig.minecraft.build.config.ConfigPersistence.save();
        }
        wasKeyDown = isKeyDown;

        if (!is.pig.minecraft.build.config.PiggyBuildConfig.getInstance().isFeatureAutoParkourEnabled() ||
            !is.pig.minecraft.build.config.PiggyBuildConfig.getInstance().isAutoParkourEnabled()) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }

        ItemStack mainHandItem = player.getMainHandItem();
        boolean isHoldingBlock = !mainHandItem.isEmpty() && (mainHandItem.getItem() instanceof BlockItem);
        boolean flashing = false;
        
        if (!isHoldingBlock) {
            flashing = true;
        } else {
            int blockCount = 0;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() == mainHandItem.getItem()) {
                    blockCount += stack.getCount();
                }
            }
            if (blockCount <= 16) {
                flashing = true;
            }
        }

        is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/auto_parkour.png"),
            1000, flashing
        );

        if (!isHoldingBlock) {
            return;
        }

        // Rule 1: Never place if jumping upwards
        // player.getDeltaMovement().y > 0 means the player is rising towards the apex
        if (player.getDeltaMovement().y > 0) {
            return;
        }

        BlockPos targetPos = null;

        // Base check block below feet
        BlockPos playerFeet = player.blockPosition();
        BlockPos directlyBelow = playerFeet.relative(Direction.DOWN);

        // Preemptive target calculation: scale lookahead by speed so fast players don't fall off edges
        net.minecraft.world.phys.Vec3 vel = player.getDeltaMovement();
        net.minecraft.world.phys.Vec3 horizontalVel = new net.minecraft.world.phys.Vec3(vel.x, 0, vel.z);
        double speed = horizontalVel.length();
        
        net.minecraft.world.phys.Vec3 forwardOffset;
        if (speed > 0.01) {
            forwardOffset = horizontalVel.normalize().scale(Math.max(0.8, speed * 3.5));
        } else {
            // Stationary fallback if jumping in place
            forwardOffset = net.minecraft.world.phys.Vec3.ZERO;
        }

        BlockPos predictedBelow = BlockPos.containing(player.getX() + forwardOffset.x, playerFeet.getY(), player.getZ() + forwardOffset.z).relative(Direction.DOWN);

        // Rule 2 & 3: Find the best target
        if (player.isSprinting() && player.onGround()) {
            // When sprinting on the ground, always try to place ahead to prevent falling
            targetPos = predictedBelow;
        }
        else if (!player.onGround() && player.getDeltaMovement().y <= 0) {
            // When falling after a jump, prioritize where they will land
            if (client.level.getBlockState(predictedBelow).canBeReplaced() && speed > 0.1) {
                targetPos = predictedBelow;
            } else {
                targetPos = directlyBelow;
            }
            
            // Failsafe: if we are over an empty gap directly below us, ensure we place there first
            if (client.level.getBlockState(directlyBelow).canBeReplaced()) {
                targetPos = directlyBelow;
            }
        }

        if (targetPos == null) {
            return;
        }

        // Must be air/replaceable
        if (!client.level.getBlockState(targetPos).canBeReplaced()) {
            return;
        }

        // Check CPS limit
        long currentTime = System.currentTimeMillis();
        int cps = is.pig.minecraft.build.config.PiggyBuildConfig.getInstance().getTickDelay();
        long minDelay = cps > 0 ? 1000L / cps : 0;

        if (currentTime - lastPlacementTime < minDelay) {
            return;
        }

        // Attempt placement
        boolean success = BlockPlacer.placeBlock(targetPos, Direction.UP, InteractionHand.MAIN_HAND, minDelay == 0);
        
        if (success) {
            lastPlacementTime = System.currentTimeMillis();
        }
    }
}
