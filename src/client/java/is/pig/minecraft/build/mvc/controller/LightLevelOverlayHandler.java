package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;

public class LightLevelOverlayHandler {
    
    private boolean active = false;
    private boolean wasPressed = false;

    public void onTick(Minecraft client) {
        if (client.player == null) return;

        boolean isPressed = InputController.lightLevelOverlayKey.isDown();

        if (isPressed && !wasPressed) {
            active = !active;
        }

        wasPressed = isPressed;

        if (active) {
            is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/light_level.png"),
                1000, false
            );
        }
    }

    public boolean isActive() {
        return active;
    }
}
