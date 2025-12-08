package is.pig.minecraft.build.mvc.controller;

import com.mojang.blaze3d.platform.InputConstants;
import is.pig.minecraft.build.lib.event.MouseScrollCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

public class InputController {

    // Static keys accessible by handlers/views
    public static KeyMapping triggerKey;
    public static KeyMapping directionalKey;
    public static KeyMapping diagonalKey;
    public static KeyMapping fastPlaceKey;

    // Handlers (Logic separation)
    private final ShapeMenuHandler menuHandler = new ShapeMenuHandler();
    private static DirectionalPlacementHandler placementHandler = new DirectionalPlacementHandler();
    private final FastPlacementHandler fastPlacementHandler = new FastPlacementHandler();
    private final FastBreakHandler fastBreakHandler = new FastBreakHandler();

    public void initialize() {
        registerKeys();
        registerEvents();
    }

    private void registerKeys() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Shape Selector",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_X,
                "Piggy Build"));

        directionalKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Directional Block Placement",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_5,
                "Piggy Build"));

        diagonalKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Diagonal Block Placement",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_4,
                "Piggy Build"));

        fastPlaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Fast Block Placement",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_6,
                "Piggy Build"));
    }

    private void registerEvents() {
        // 1. Scroll Event -> Delegated to Handlers
        MouseScrollCallback.EVENT.register(fastPlacementHandler::onScroll);
        MouseScrollCallback.EVENT.register(menuHandler::onScroll);

        // 2. Client Tick -> Delegated to both handlers
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            menuHandler.onTick(client);
            placementHandler.onTick(client);
            fastPlacementHandler.onTick(client);
            fastBreakHandler.onTick(client);
        });

        // 3. Block placement is now handled via MinecraftClientMixin
        // The mixin calls getDirectionalPlacementHandler() to modify hit results
    }

    // Static accessor for the mixin to use
    public static DirectionalPlacementHandler getDirectionalPlacementHandler() {
        return placementHandler;
    }
}