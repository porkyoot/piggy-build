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
    public static KeyMapping flexibleKey;
    public static KeyMapping adjacentKey;
    public static KeyMapping fastPlaceKey;

    // Handlers (Logic separation)
    private final ShapeMenuHandler menuHandler = new ShapeMenuHandler();
    private static FlexiblePlacementHandler placementHandler = new FlexiblePlacementHandler();
    private final FastPlacementHandler fastPlacementHandler = new FastPlacementHandler();

    public void initialize() {
        registerKeys();
        registerEvents();
    }

    private void registerKeys() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Shape Selector", 
            InputConstants.Type.KEYSYM, 
            GLFW.GLFW_KEY_X, 
            "Piggy Build"
        ));
        
        flexibleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Flexible Block Placement", 
            InputConstants.Type.MOUSE, 
            GLFW.GLFW_MOUSE_BUTTON_5, 
            "Piggy Build"
        ));
        
        adjacentKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Adjacent Block Placement", 
            InputConstants.Type.MOUSE, 
            GLFW.GLFW_MOUSE_BUTTON_4, 
            "Piggy Build"
        ));
        
        fastPlaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Fast Block Placement", 
            InputConstants.Type.KEYSYM, 
            GLFW.GLFW_KEY_LEFT_CONTROL, 
            "Piggy Build"
        ));
    }

    private void registerEvents() {
        // 1. Scroll Event -> Delegated to Menu Handler
        MouseScrollCallback.EVENT.register(menuHandler::onScroll);

        // 2. Client Tick -> Delegated to both handlers
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            menuHandler.onTick(client);
            placementHandler.onTick(client);
            fastPlacementHandler.onTick(client);
        });

        // 3. Block placement is now handled via MinecraftClientMixin
        // The mixin calls getFlexiblePlacementHandler() to modify hit results
    }

    // Static accessor for the mixin to use
    public static FlexiblePlacementHandler getFlexiblePlacementHandler() {
        return placementHandler;
    }
}