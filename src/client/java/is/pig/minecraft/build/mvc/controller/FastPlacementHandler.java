package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.config.PiggyConfig;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import is.pig.minecraft.build.mvc.view.FastPlaceOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Handles fast block placement by reducing or removing the click delay.
 * This is a toggle mode - press the key to turn it on/off.
 */
public class FastPlacementHandler {

    private boolean wasKeyDown = false;
    private long lastPlacementTime = 0;
    private long lastWarningTime = 0;

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

        // If fast place is enabled, try to place continuously
        if (PiggyConfig.getInstance().isFastPlaceEnabled()) {
            tryFastPlace(client);
        }
    }

    /**
     * Toggle fast placement mode on/off
     */
    private void toggleFastPlace() {
        PiggyConfig config = PiggyConfig.getInstance();
        config.setFastPlaceEnabled(!config.isFastPlaceEnabled());
        ConfigPersistence.save();

        if (config.isFastPlaceEnabled()) {
            PiggyBuildClient.LOGGER.debug("[FastPlace] Enabled");
        } else {
            PiggyBuildClient.LOGGER.debug("[FastPlace] Disabled");
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
        PiggyConfig config = PiggyConfig.getInstance();
        if (!config.isFeatureFastPlaceEnabled()) {
            warnFeatureDisabled(client);
            return;
        }

        // Check if we're looking at a block
        if (!(client.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        // Check if enough time has passed since last placement
        long currentTime = System.currentTimeMillis();
        long minDelay = PiggyConfig.getInstance().getFastPlaceDelayMs();

        if (currentTime - lastPlacementTime < minDelay) {
            return;
        }

        // Check if right mouse button is held down
        if (!client.options.keyUse.isDown()) {
            return;
        }

        // Perform the placement
        performFastPlace(client, blockHit);
    }

    /**
     * Actually perform the block placement
     */
    private void performFastPlace(Minecraft client, BlockHitResult hitResult) {
        LocalPlayer player = client.player;

        // Override the delay to 0 for instant placement
        ((MinecraftAccessorMixin) client).setRightClickDelay(0);

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

            // If directional or diagonal mode is active, modify the hit result
            if (directionalActive || diagonalActive) {
                DirectionalPlacementHandler handler = InputController.getDirectionalPlacementHandler();
                if (handler != null) {
                    finalHitResult = handler.modifyHitResult(client, hitResult);
                }
            }

            // Perform the placement through the game mode with the (possibly modified) hit
            // result
            InteractionResult result = client.gameMode.useItemOn(player, hand, finalHitResult);

            if (result.consumesAction()) {
                // Notify overlay for cooldown tracking
                FastPlaceOverlay.onFastPlace();

                // Swing animation
                if (result.shouldSwing()) {
                    player.swing(hand);
                }
            }

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastPlace] Placement error", e);
        }
    }

    /**
     * Check if fast placement is currently active
     */
    public boolean isActive() {
        return PiggyConfig.getInstance().isFastPlaceEnabled();
    }

    /**
     * Handles scroll wheel to change fast placement delay.
     */
    public boolean onScroll(double amount) {
        if (InputController.fastPlaceKey.isDown()) {
            PiggyConfig config = PiggyConfig.getInstance();

            // 1. Auto-enable if currently disabled
            if (!config.isFastPlaceEnabled()) {
                config.setFastPlaceEnabled(true);
                PiggyBuildClient.LOGGER.debug("[FastPlace] Enabled via scroll");
            }

            // 2. Adjust Speed (Blocks Per Second) instead of raw delay
            int currentDelay = config.getFastPlaceDelayMs();
            // Calculate current speed (avoid div/0, min delay is 50ms so max speed is 20)
            int currentSpeed = 1000 / Math.max(1, currentDelay);

            // Adjust speed: +1 for Scroll Up, -1 for Scroll Down
            int change = amount > 0 ? 1 : -1;
            int newSpeed = currentSpeed + change;

            // Clamp speed: Min 1 block/sec (1000ms), Max 20 blocks/sec (50ms)
            if (newSpeed < 1)
                newSpeed = 1;
            if (newSpeed > 20)
                newSpeed = 20;

            // Convert back to delay (ms)
            int newDelay = 1000 / newSpeed;

            if (newDelay != currentDelay) {
                config.setFastPlaceDelayMs(newDelay);
                config.setFastBreakDelayMs(newDelay); // Sync break delay
                ConfigPersistence.save();

                PiggyBuildClient.LOGGER
                        .info("Config updated - New Delay: " + newDelay + "ms (Speed: " + newSpeed + ")");

                // 3. Simplified Feedback
                LocalPlayer player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "Fast Build Speed: " + newSpeed + " blocks/sec"), true);
                }
            }
            return true;
        }
        return false;
    }

    private void warnFeatureDisabled(Minecraft client) {
        // Only warn if the user is ACTUALLY trying to use it (holding use key)
        if (!client.options.keyUse.isDown()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        // Warn every 2 seconds max
        if (currentTime - lastWarningTime > 2000) {
            lastWarningTime = currentTime;
            if (client.player != null) {
                PiggyConfig config = PiggyConfig.getInstance();
                boolean serverForces = !config.serverAllowCheats ||
                        (config.serverFeatures.containsKey("fast_place") && !config.serverFeatures.get("fast_place"));

                String message = serverForces
                        ? "Fast Place Disabled: This server has disabled this feature."
                        : "Fast Place Disabled: Enable in mod settings or disable 'No Cheating Mode'.";

                client.player.displayClientMessage(
                        Component.literal(message)
                                .withStyle(ChatFormatting.RED),
                        true // true = action bar
                );
            }
        }
    }
}