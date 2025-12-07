package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles fast block breaking when fast placement is active and player is in
 * creative mode.
 * Spams the break block action while the attack button is held down.
 */
public class FastBreakHandler {

    private long lastBreakTime = 0;

    // Track recently broken blocks to prevent ghost blocks
    private final Map<BlockPos, Long> recentlyBroken = new HashMap<>();
    private static final long GHOST_PREVENTION_MS = 100; // Don't re-break same block for 100ms

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

        // Clean up old entries from recently broken map
        long currentTime = System.currentTimeMillis();
        recentlyBroken.entrySet().removeIf(entry -> currentTime - entry.getValue() > GHOST_PREVENTION_MS);

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

        BlockPos blockPos = blockHit.getBlockPos();

        // Don't break the same block we just broke (ghost block prevention)
        long currentTime = System.currentTimeMillis();
        if (recentlyBroken.containsKey(blockPos)) {
            long timeSinceBreak = currentTime - recentlyBroken.get(blockPos);
            if (timeSinceBreak < GHOST_PREVENTION_MS) {
                return; // Skip this block, it was recently broken
            }
        }

        // Check if enough time has passed since last break
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
            // Verify the block exists client-side before trying to break it
            if (client.level.getBlockState(blockPos).isAir()) {
                PiggyBuildClient.LOGGER.debug("[FastBreak] Block at {} is already air (ghost block), skipping",
                        blockPos);
                return;
            }

            // In creative mode, we can instantly destroy blocks
            boolean success = client.gameMode.destroyBlock(blockPos);

            if (success) {
                // Update last break time
                lastBreakTime = System.currentTimeMillis();

                // Track this position to prevent re-breaking
                recentlyBroken.put(blockPos.immutable(), lastBreakTime);

                // Swing animation
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

                PiggyBuildClient.LOGGER.debug("[FastBreak] Broke block at {}", blockPos);
            }

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastBreak] Breaking error", e);
        }
    }
}
