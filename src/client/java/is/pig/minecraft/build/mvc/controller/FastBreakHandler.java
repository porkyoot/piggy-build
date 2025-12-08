package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.config.PiggyConfig;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
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
        long minDelay = PiggyConfig.getInstance().getFastBreakDelayMs();

        if (currentTime - lastBreakTime < minDelay) {
            // PiggyBuildClient.LOGGER.info("Skipping break: Delta=" + (currentTime -
            // lastBreakTime) + " < MinDelay=" + minDelay);
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
    /**
     * Actually perform the block breaking
     */
    private void performFastBreak(Minecraft client, BlockHitResult hitResult) {
        // Reset vanilla attack cooldown/delay
        ((MinecraftAccessorMixin) client).setMissTime(0);

        try {
            // Simulate the attack button press via vanilla logic
            // This handles swinging, particles, range checks, and sending the correct
            // packet
            boolean success = ((MinecraftAccessorMixin) client).invokeStartAttack();

            if (success) {
                // Update last break time
                lastBreakTime = System.currentTimeMillis();

                // Track this position to prevent re-breaking ghost blocks locally
                recentlyBroken.put(hitResult.getBlockPos().immutable(), lastBreakTime);

                PiggyBuildClient.LOGGER.debug("[FastBreak] Broke block at {}", hitResult.getBlockPos());
            }

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastBreak] Breaking error", e);
        }
    }
}
