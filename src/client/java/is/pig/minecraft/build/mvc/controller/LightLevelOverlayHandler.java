package is.pig.minecraft.build.mvc.controller;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;
import is.pig.minecraft.lib.ui.IconQueueOverlay;

public class LightLevelOverlayHandler {
    
    private boolean active = false;
    private boolean wasPressed = false;

    public void onTick(Object client) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        boolean isPressed = input.isKeyDown("piggy-build:light_level");

        if (isPressed && !wasPressed) {
            active = !active;
        }

        wasPressed = isPressed;

        if (active) {
            IconQueueOverlay.queueIcon(
                ResourceLocation.of("piggy", "textures/gui/icons/light_level.png"),
                1000, false
            );
        }
    }

    public boolean isActive() {
        return active;
    }
}
