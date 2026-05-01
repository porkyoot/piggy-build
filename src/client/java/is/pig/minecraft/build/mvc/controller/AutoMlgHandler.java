package is.pig.minecraft.build.mvc.controller;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.mlg.statemachine.MlgState;
import is.pig.minecraft.build.mlg.statemachine.MlgStateMachine;
import is.pig.minecraft.lib.ui.IconQueueOverlay;

public class AutoMlgHandler {
    
    private boolean wasKeyDown = false;

    public void onTick(Object client) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        boolean isKeyDown = input.isKeyDown("piggy-build:auto_mlg");

        if (isKeyDown && !wasKeyDown) {
            PiggyBuildConfig config = PiggyBuildConfig.getInstance();
            boolean newState = !config.isAutoMlgEnabled();
            config.setAutoMlgEnabled(newState);
            ConfigPersistence.save();

            if (config.isAutoMlgEnabled() == newState && newState) {
                IconQueueOverlay.queueIcon(
                    ResourceLocation.of("piggy", "textures/gui/icons/auto_mlg.png"),
                    1000, false
                );
            }
        }
        wasKeyDown = isKeyDown;

        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        if (!config.isFeatureAutoMlgEnabled() || !config.isAutoMlgEnabled()) {
            return;
        }

        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        if (MlgStateMachine.getInstance().hasActiveMethod()) {
            IconQueueOverlay.queueIcon(
                ResourceLocation.of("piggy", "textures/gui/icons/auto_mlg.png"),
                1000, true
            );
        }
        
        if (MlgStateMachine.getInstance().getCurrentState() == MlgState.IDLE) {
            if (worldState.isOnGround(client) || !worldState.isFalling(client)) {
                return;
            }
        }

        MlgStateMachine.getInstance().tick(client);
    }
}
