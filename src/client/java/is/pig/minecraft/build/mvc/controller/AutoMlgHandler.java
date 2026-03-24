package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.mlg.statemachine.MlgStateMachine;
import net.minecraft.client.Minecraft;

public class AutoMlgHandler {
    
    private boolean wasKeyDown = false;

    public void onTick(Minecraft client) {
        if (is.pig.minecraft.build.mvc.controller.InputController.autoMlgKey == null) return;
        
        // Toggle logic
        boolean isKeyDown = is.pig.minecraft.build.mvc.controller.InputController.autoMlgKey.isDown();
        if (isKeyDown && !wasKeyDown) {
            is.pig.minecraft.build.config.PiggyBuildConfig config = is.pig.minecraft.build.config.PiggyBuildConfig.getInstance();
            boolean newState = !config.isAutoMlgEnabled();
            config.setAutoMlgEnabled(newState);
            is.pig.minecraft.build.config.ConfigPersistence.save();

            if (client.player != null) {
                // If it successfully toggled (was not blocked by AntiCheat), show icon
                if (config.isAutoMlgEnabled() == newState && newState) {
                    // Display and fade the icon once to confirm enabling
                    is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/auto_mlg.png"),
                        1000, false
                    );
                }
            }
        }
        wasKeyDown = isKeyDown;

        if (!is.pig.minecraft.build.config.PiggyBuildConfig.getInstance().isFeatureAutoMlgEnabled() ||
            !is.pig.minecraft.build.config.PiggyBuildConfig.getInstance().isAutoMlgEnabled()) {
            return;
        }

        if (client.player == null || client.level == null) return;
        
        // Flash the MLG icon only if the State Machine has detected a fall and is actively handling it
        if (MlgStateMachine.getInstance().getCurrentState() != is.pig.minecraft.build.mlg.statemachine.MlgState.IDLE) {
            is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/auto_mlg.png"),
                1000, true
            );
        }
        
        if (MlgStateMachine.getInstance().getCurrentState() == is.pig.minecraft.build.mlg.statemachine.MlgState.IDLE) {
            if (client.player.onGround() || client.player.getDeltaMovement().y >= 0 || client.player.isFallFlying()) {
                return;
            }
        }

        MlgStateMachine.getInstance().tick(client);
    }
}
