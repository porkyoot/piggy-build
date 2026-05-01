package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.lib.action.BulkAction;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.ui.IconQueueOverlay;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AutoParkourHandler {

    private long lastPlacementTime = 0;
    private boolean wasKeyDown = false;
    
    private final Map<BlockPos, Long> pendingVerifications = new HashMap<>();

    public void onTick(Object client) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        boolean isKeyDown = input.isKeyDown("piggy-build:auto_parkour");

        if (isKeyDown && !wasKeyDown) {
            PiggyBuildConfig config = PiggyBuildConfig.getInstance();
            boolean newState = !config.isAutoParkourEnabled();
            config.setAutoParkourEnabled(newState);
            ConfigPersistence.save();

            if (config.isAutoParkourEnabled() == newState && newState) {
                IconQueueOverlay.queueIcon(
                    ResourceLocation.of("piggy", "textures/gui/icons/auto_parkour.png"),
                    1000, false
                );
            }
        }
        wasKeyDown = isKeyDown;

        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        if (!config.isFeatureAutoParkourEnabled() || !config.isAutoParkourEnabled()) {
            return;
        }

        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        long now = System.currentTimeMillis();
        String worldId = worldState.getCurrentWorldId();
        
        pendingVerifications.entrySet().removeIf(entry -> {
            if (now >= entry.getValue()) {
                BlockPos pos = entry.getKey();
                if (worldState.isBlockReplaceable(worldId, pos)) {
                    worldState.sendMessage(client, "§cAutoParkour block reverted! Falling back to MLG...");
                }
                return true;
            }
            return false;
        });

        if (worldState.isPlayerDeadOrDying(client)) return;

        Object mainHandItem = worldState.getPlayerMainHandItem(client);
        boolean isHoldingBlock = PiggyServiceRegistry.getItemDataAdapter().isBlockItem(mainHandItem);
        
        if (!isHoldingBlock) return;

        if (worldState.getPlayerDeltaMovement(client).y() > 0) return;

        BlockPos playerPos = worldState.getPlayerBlockPos(client);
        Vec3 vel = worldState.getPlayerDeltaMovement(client);
        Vec3 horizontalVel = new Vec3(vel.x(), 0, vel.z());
        double speed = Math.sqrt(horizontalVel.x() * horizontalVel.x() + horizontalVel.z() * horizontalVel.z());
        
        Vec3 forwardOffset;
        if (speed > 0.01) {
            int ping = worldState.getPing();
            double latencyOffset = speed * (ping / 1000.0);
            double scale = Math.max(0.8, speed * 3.5) + latencyOffset;
            forwardOffset = new Vec3((horizontalVel.x() / speed) * scale, 0, (horizontalVel.z() / speed) * scale);
        } else {
            forwardOffset = new Vec3(0, 0, 0);
        }

        Vec3 playerVecPos = worldState.getPlayerPosition(client);
        BlockPos predictedBelow = new BlockPos(
            (int) Math.floor(playerVecPos.x() + forwardOffset.x()),
            (int) Math.floor(playerVecPos.y() - 1),
            (int) Math.floor(playerVecPos.z() + forwardOffset.z())
        );

        BlockPos directlyBelow = new BlockPos(playerPos.x(), playerPos.y() - 1, playerPos.z());
        BlockPos targetPos = null;

        if (worldState.isPlayerSprinting(client) && worldState.isPlayerOnGround(client)) {
            targetPos = predictedBelow;
        } else if (!worldState.isPlayerOnGround(client) && vel.y() <= 0) {
            if (worldState.isBlockReplaceable(worldId, predictedBelow) && speed > 0.1) {
                targetPos = predictedBelow;
            } else {
                targetPos = directlyBelow;
            }
            
            if (worldState.isBlockReplaceable(worldId, directlyBelow)) {
                targetPos = directlyBelow;
            }
        }

        if (targetPos == null || !worldState.isBlockReplaceable(worldId, targetPos)) return;

        long currentTime = System.currentTimeMillis();
        int cps = config.getTickDelay();
        long minDelay = cps > 0 ? 1000L / cps : 0;

        if (currentTime - lastPlacementTime < minDelay) return;

        boolean ignoreGlobalCps = (cps <= 0);
        BlockHitResult hitResult = BlockPlacer.createHitResult(targetPos, Direction.UP);
        Action act = BlockPlacer.createAction(hitResult, InteractionHand.MAIN_HAND, ignoreGlobalCps);
        
        if (act != null) {
            var bulkAction = new BulkAction(
                    "piggy-build-parkour",
                    ActionPriority.HIGH,
                    Collections.singletonList(act),
                    () -> true,
                    0, 
                    "Auto Parkour Placement"
            );
            
            if (ignoreGlobalCps) bulkAction.setIgnoreGlobalCps(true);
            PiggyActionQueue.getInstance().enqueue(bulkAction);

            lastPlacementTime = currentTime;
            int ping = worldState.getPing();
            long verificationDelay = 250 + (ping > 150 ? ping : 0);
            pendingVerifications.put(targetPos, currentTime + verificationDelay); 
        }
    }
}
