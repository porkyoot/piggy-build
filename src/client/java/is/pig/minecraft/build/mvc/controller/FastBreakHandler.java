package is.pig.minecraft.build.mvc.controller;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.world.BreakBlockAction;

import java.util.HashMap;
import java.util.Map;

public class FastBreakHandler {

    private long lastBreakTime = 0;
    private final Map<BlockPos, Long> recentlyBroken = new HashMap<>();
    private static final long GHOST_PREVENTION_MS = 100;

    public void onTick(Object client) {
        if (!PiggyBuildConfig.getInstance().isFastPlaceEnabled()) {
            return;
        }

        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        if (!worldState.isCreative(client)) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        recentlyBroken.entrySet().removeIf(entry -> currentTime - entry.getValue() > GHOST_PREVENTION_MS);

        tryFastBreak(client);
    }

    private void tryFastBreak(Object client) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        HitResult hitResult = worldState.getCrosshairTarget(client);

        if (!(hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos blockPos = blockHit.blockPos();

        long currentTime = System.currentTimeMillis();
        if (recentlyBroken.containsKey(blockPos)) {
            long timeSinceBreak = currentTime - recentlyBroken.get(blockPos);
            if (timeSinceBreak < GHOST_PREVENTION_MS) {
                return;
            }
        }

        if (PiggyActionQueue.getInstance().hasActions("piggy-build-fastbreak")) {
            return;
        }

        int cps = PiggyBuildConfig.getInstance().getTickDelay();
        long minDelay = cps > 0 ? 1000L / cps : 0;

        if (currentTime - lastBreakTime < minDelay) {
            return;
        }

        boolean unlimited = (cps <= 0);

        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        if (!input.isKeyDown("minecraft:attack")) {
            return;
        }

        performFastBreak(client, blockHit, unlimited);
    }

    private void performFastBreak(Object client, BlockHitResult hitResult, boolean unlimited) {
        try {
            var action = new BreakBlockAction(
                    hitResult.blockPos(),
                    "piggy-build-fastbreak",
                    ActionPriority.NORMAL
            );
            if (unlimited) {
                action.setIgnoreGlobalCps(true);
            }
            PiggyActionQueue.getInstance().enqueue(action);

            long currentTime = System.currentTimeMillis();
            lastBreakTime = currentTime;
            recentlyBroken.put(hitResult.blockPos(), currentTime);

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[FastBreak] Breaking error", e);
        }
    }
}
