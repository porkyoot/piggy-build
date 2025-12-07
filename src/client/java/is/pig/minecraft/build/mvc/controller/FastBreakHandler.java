package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Handles fast block breaking when fast placement is active and player is in
 * creative mode.
 * Spams the break block action while the attack button is held down.
 */
public class FastBreakHandler {

    private long lastBreakTime = 0;

    /**
     * Called every client tick to handle fast breaking logic
     */
    public void onTick(Minecraft client) {
        // Only work when fast placement is enabled
        if (!PiggyConfig.getInstance().isFastPlaceEnabled()) {
            return;
        }

        // Only work in creative mode
        if (client.player == null || client.gameMode == null) {
            return;
        }

        if (client.gameMode.getPlayerMode() != GameType.CREATIVE) {
            return;
        }

        // Try to break blocks continuously
        tryFastBreak(client);
    }

    /**
     * Attempt to break a block if conditions are met
     */
    private void tryFastBreak(Minecraft client) {
        // Check if we're looking at a block
        if (!(client.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        if (blockHit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        // Check if enough time has passed since last break
        long currentTime = System.currentTimeMillis();
        long minDelay = PiggyConfig.getInstance().getFastPlaceDelayMs();

        if (currentTime - lastBreakTime < minDelay) {
            return;
        }

        // Check if left mouse button (attack) is held down
        if (!client.options.keyAttack.isDown()) {
            return;
        }

        // Perform the break
        performFastBreak(client, blockHit);
    }

    /**
     * Actually perform the block breaking
     */
    private void performFastBreak(Minecraft client, BlockHitResult hitResult) {
        LocalPlayer player = client.player;
        BlockPos blockPos = hitResult.getBlockPos();

        try {
            // In creative mode, we can instantly destroy blocks
            // Use continueDestroyBlock which handles creative mode properly
            boolean success = client.gameMode.destroyBlock(blockPos);

            if (success) {
                // Update last break time
                lastBreakTime = System.currentTimeMillis();

                // Swing animation
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

                PiggyBuildClient.LOGGER.debug("[FastBreak] Broke block at {}", blockPos);
            }

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastBreak] Breaking error", e);
        }
    }
}
