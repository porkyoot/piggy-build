package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class LightLevelOverlayHandler {
    
    private boolean active = false;
    private boolean wasPressed = false;

    public void onTick(Minecraft client) {
        if (client.player == null) return;

        boolean isPressed = InputController.lightLevelOverlayKey.isDown();

        if (isPressed && !wasPressed) {
            active = !active;
            client.player.displayClientMessage(Component.literal("Light Level Overlay: " + (active ? "ON" : "OFF")), true);
        }

        wasPressed = isPressed;
    }

    public boolean isActive() {
        return active;
    }
}
