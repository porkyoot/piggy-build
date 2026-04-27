package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.api.IPlayerEntity;
import is.pig.minecraft.build.api.IWorldAccess;

/**
 * Input handler for Auto MLG mechanics.
 * Pure Java, zero Minecraft dependencies.
 */
public class AutoMlgHandler {
    
    private boolean wasKeyDown = false;

    public void onTick(IPlayerEntity player, IWorldAccess world) {
        boolean isKeyDown = world.isAutoMlgKeyDown();
        
        // Toggle logic
        if (isKeyDown && !wasKeyDown) {
            boolean newState = !world.isAutoMlgEnabled();
            world.setAutoMlgEnabled(newState);
            world.saveConfig();

            if (player != null) {
                if (world.isAutoMlgEnabled() == newState && newState) {
                    world.queueIcon("piggy", "textures/gui/icons/auto_mlg.png", 1000, false);
                }
            }
        }
        wasKeyDown = isKeyDown;

        if (!world.isFeatureAutoMlgEnabled() || !world.isAutoMlgEnabled()) {
            return;
        }

        if (player == null) return;
        
        // Flash the MLG icon if state machine has active method
        if (world.hasActiveMlgMethod()) {
            world.queueIcon("piggy", "textures/gui/icons/auto_mlg.png", 1000, true);
        }
        
        if (world.isMlgStateMachineIdle()) {
            if (player.onGround() || player.getDeltaMovement().y() >= 0 || player.isFallFlying()) {
                return;
            }
        }

        world.tickMlgStateMachine();
    }
}
