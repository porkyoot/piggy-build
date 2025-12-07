package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Handles fast block placement by reducing or removing the click delay.
 * When the fast placement key is held, blocks can be placed rapidly.
 */
public class FastPlacementHandler {

    private boolean wasKeyDown = false;
    private int originalDelay = 4;
    private long lastPlacementTime = 0;
    
    // Configuration: minimum delay between placements (in milliseconds)
    // 0 = instant, 50 = 20 blocks/sec, 100 = 10 blocks/sec
    private static final long MIN_DELAY_MS = 0;

    /**
     * Called every client tick to handle fast placement logic
     */
    public void onTick(Minecraft client) {
        boolean isKeyDown = InputController.fastPlaceKey.isDown();
        
        // Key just pressed
        if (isKeyDown && !wasKeyDown) {
            onFastPlaceEnabled(client);
        }
        
        // Key just released
        if (!isKeyDown && wasKeyDown) {
            onFastPlaceDisabled(client);
        }
        
        // Key is held down - try to place continuously
        if (isKeyDown) {
            tryFastPlace(client);
        }
        
        wasKeyDown = isKeyDown;
    }

    /**
     * Called when fast placement is enabled
     */
    private void onFastPlaceEnabled(Minecraft client) {
        // Store the original delay
        originalDelay = ((MinecraftAccessorMixin) client).getRightClickDelay();
        PiggyBuildClient.LOGGER.info("[FastPlace] Enabled - Original delay: " + originalDelay);
    }

    /**
     * Called when fast placement is disabled
     */
    private void onFastPlaceDisabled(Minecraft client) {
        // Restore the original delay
        ((MinecraftAccessorMixin) client).setRightClickDelay(originalDelay);
        PiggyBuildClient.LOGGER.info("[FastPlace] Disabled - Restored delay: " + originalDelay);
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
        if (currentTime - lastPlacementTime < MIN_DELAY_MS) {
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
            
            // Perform the placement through the game mode
            InteractionResult result = client.gameMode.useItemOn(player, hand, hitResult);
            
            if (result.consumesAction()) {
                // Update last placement time
                lastPlacementTime = System.currentTimeMillis();
                
                // Swing animation
                if (result.shouldSwing()) {
                    player.swing(hand);
                }
                
                PiggyBuildClient.LOGGER.debug("[FastPlace] Block placed successfully");
            }
            
        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastPlace] Placement error", e);
        }
    }

    /**
     * Check if fast placement is currently active
     */
    public boolean isActive() {
        return InputController.fastPlaceKey.isDown();
    }

    /**
     * Set the minimum delay between placements (in milliseconds)
     * Useful for adjusting the speed based on user preference
     */
    public static void setMinDelayMs(long delayMs) {
        // Note: This would need to be made non-static and configurable
        // Left as static constant for now, but this shows how to make it configurable
    }
}