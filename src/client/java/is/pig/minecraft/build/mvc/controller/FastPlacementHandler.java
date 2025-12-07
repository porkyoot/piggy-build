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

/**
 * Handles fast block placement by reducing or removing the click delay.
 * This is a toggle mode - press the key to turn it on/off.
 */
public class FastPlacementHandler {

    private boolean wasKeyDown = false;
    private long lastPlacementTime = 0;

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
                    PiggyBuildClient.LOGGER.debug("[FastPlace] Using modified hit result from " +
                            (directionalActive ? "DIRECTIONAL" : "DIAGONAL") + " mode");
                }
            }

            // Perform the placement through the game mode with the (possibly modified) hit
            // result
            InteractionResult result = client.gameMode.useItemOn(player, hand, finalHitResult);

            if (result.consumesAction()) {
                // Update last placement time
                lastPlacementTime = System.currentTimeMillis();

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
}