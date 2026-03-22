package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import is.pig.minecraft.lib.ui.IconQueueOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Handles fast block placement by reducing or removing the click delay.
 * This is a toggle mode - press the key to turn it on/off.
 */
public class FastPlacementHandler {

    private boolean wasKeyDown = false;
    private long lastPlacementTime = 0;

    // Track recently placed blocks to prevent ghost blocks
    private final java.util.Map<net.minecraft.core.BlockPos, Long> recentlyPlaced = new java.util.HashMap<>();
    private static final long GHOST_PREVENTION_MS = 100; // Don't re-place same block for 100ms

    /**
     * Called every client tick to handle fast placement logic
     */
    public void onTick(Minecraft client) {
        boolean isKeyDown = InputController.fastPlaceKey.isDown();

        // Key just pressed - toggle the mode
        if (isKeyDown && !wasKeyDown) {
            toggleFastPlace();
        }

        wasKeyDown = isKeyDown;

        // Clean up old entries from recently placed map
        long currentTime = System.currentTimeMillis();
        recentlyPlaced.entrySet().removeIf(entry -> currentTime - entry.getValue() > GHOST_PREVENTION_MS);

        // If fast place is enabled, try to place continuously
        if (PiggyBuildConfig.getInstance().isFastPlaceEnabled()) {
            tryFastPlace(client);
        }
    }

    /**
     * Toggle fast placement mode on/off
     */
    private void toggleFastPlace() {
        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        boolean newState = !config.isFastPlaceEnabled();
        config.setFastPlaceEnabled(newState);
        // Only persist and log if the setter actually changed the state
        if (config.isFastPlaceEnabled() == newState) {
            ConfigPersistence.save();
        }
    }

    /**
     * Attempt to place a block if conditions are met
     */
    private void tryFastPlace(Minecraft client) {
        // Check basic conditions
        if (client.player == null || client.gameMode == null) {
            return;
        }

        // Check if feature is enabled (considers server overrides)
        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        boolean isEnabled = config.isFeatureFastPlaceEnabled();

        if (!isEnabled) {
            // If the feature is disabled, just return silently. Feedback is shown
            // when the user attempts to ENABLE the feature (via setter/toggle).
            return;
        }

        // Check if we're looking at a block
        if (!(client.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        // Suppress vanilla placement unconditionally when fast place is active
        // and pointing at block, let this handler do the rest.
        ((MinecraftAccessorMixin) client).setRightClickDelay(100);

        // Check if enough time has passed since last placement
        long currentTime = System.currentTimeMillis();
        int cps = PiggyBuildConfig.getInstance().getTickDelay();
        long minDelay = cps > 0 ? 1000L / cps : 0;

        // Allow up to 25ms of jitter to align with ticks, since client tick is 50ms
        if (minDelay > 0 && (currentTime - lastPlacementTime + 25) < minDelay) {
            return;
        }

        // Check if right mouse button is held down
        if (!client.options.keyUse.isDown()) {
            return;
        }

        // Perform the placement
        performFastPlace(client, blockHit);
    }

    public void notifyPlacement() {
        this.lastPlacementTime = System.currentTimeMillis();
    }

    /**
     * Actually perform the block placement
     */
    private void performFastPlace(Minecraft client, BlockHitResult hitResult) {

        // Update last placement time immediately to enforce rate limit
        // We do this regardless of success to prevent spamming the server/client logic
        lastPlacementTime = System.currentTimeMillis();

        try {
            // Use the main hand for placement
            InteractionHand hand = InteractionHand.MAIN_HAND;

            // Check if directional or diagonal mode is active
            boolean directionalActive = InputController.directionalKey.isDown();
            boolean diagonalActive = InputController.diagonalKey.isDown();

            BlockHitResult finalHitResult = hitResult;

            DirectionalPlacementHandler handler = null;
            if (directionalActive || diagonalActive) {
                handler = InputController.getDirectionalPlacementHandler();
                if (handler != null) {
                    finalHitResult = handler.modifyHitResult(client, hitResult);
                }
            }
            
            int cps = PiggyBuildConfig.getInstance().getTickDelay();
            boolean ignoreGlobalCps = (cps <= 0);

            if (finalHitResult == null) {
                return;
            }

            net.minecraft.core.BlockPos blockPos = finalHitResult.getBlockPos();
            if (finalHitResult.getType() == HitResult.Type.BLOCK) {
                boolean replaceClicked = client.level != null && client.level.getBlockState(blockPos).canBeReplaced();
                blockPos = replaceClicked ? blockPos : blockPos.relative(finalHitResult.getDirection());
            }

            // Don't place at the same position we just placed at (ghost block prevention)
            long currentTime = System.currentTimeMillis();
            if (recentlyPlaced.containsKey(blockPos)) {
                long timeSincePlace = currentTime - recentlyPlaced.get(blockPos);
                if (timeSincePlace < GHOST_PREVENTION_MS) {
                    return; // Skip this block, it was recently placed locally
                }
            }

            // Perform the placement through the centralized block placer which queues an InteractBlockAction
            boolean success = is.pig.minecraft.build.lib.placement.BlockPlacer.placeBlock(finalHitResult, hand, ignoreGlobalCps);

            if (success) {
                // Advance the session lock ONLY if the block placed actually consumed an action
                // locally
                if (handler != null) {
                    handler.onBlockPlaced(finalHitResult);
                }

                // Notify overlay for cooldown tracking
                IconQueueOverlay.queueIcon(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/fast_place.png"),
                    1000, false
                );

                // Track this position to prevent re-placing ghost blocks locally
                recentlyPlaced.put(blockPos.immutable(), System.currentTimeMillis());
            }

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastPlace] Placement error", e);
        }
    }

    /**
     * Check if fast placement is currently active
     */
    public boolean isActive() {
        return PiggyBuildConfig.getInstance().isFastPlaceEnabled();
    }

    /**
     * Handles scroll wheel to change fast placement delay.
     */
    public boolean onScroll(double amount) {
        if (InputController.fastPlaceKey.isDown()) {
            PiggyBuildConfig config = PiggyBuildConfig.getInstance();

            // 1. Auto-enable if currently disabled
            if (!config.isFastPlaceEnabled()) {
                config.setFastPlaceEnabled(true);
            }

            // 2. Adjust Speed (Blocks Per Second) (CPS)
            int currentCps = config.getTickDelay();
            int newCps = currentCps;

            // Adjust speed: +1 for Scroll Up, -1 for Scroll Down
            if (amount > 0) {
                // Scroll Up: Increase speed (up to 20, then 0 for unlimited)
                if (currentCps == 0) {
                    newCps = 0; // Already maxed at unlimited
                } else if (currentCps >= 20) {
                    newCps = 0; // Jump to unlimited
                } else {
                    newCps = currentCps + 1;
                }
            } else {
                // Scroll Down: Decrease speed
                if (currentCps <= 0) {
                    newCps = 20; // Jump down from unlimited to 20
                } else {
                    newCps = Math.max(1, currentCps - 1);
                }
            }

            if (newCps != currentCps) {
                config.setTickDelay(newCps);
                config.save();

                PiggyBuildClient.LOGGER
                        .info("Config updated - New CPS: " + newCps + " blocks/sec");

                // 3. Simplified Feedback
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    is.pig.minecraft.lib.util.PiggyMessenger.sendClientMessage(
                            player, "piggy.build.fast_place.speed_update", newCps);
                }
            }
            return true;
        }
        return false;
    }
}