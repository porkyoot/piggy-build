package is.pig.minecraft.build.mvc.controller;

import com.mojang.blaze3d.platform.InputConstants;
import is.pig.minecraft.build.lib.event.MouseScrollCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import org.lwjgl.glfw.GLFW;

public class InputController {

    // Static keys accessible by handlers/views
    public static KeyMapping triggerKey;
    public static KeyMapping flexibleKey;

    // Handlers (Logic separation)
    private final ShapeMenuHandler menuHandler = new ShapeMenuHandler();
    private final FlexiblePlacementHandler placementHandler = new FlexiblePlacementHandler();

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
        });

        // 3. Use Block (Right Click) -> Delegated to Placement Handler
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide && hand == InteractionHand.MAIN_HAND) {
                Minecraft client = Minecraft.getInstance();
                BlockHitResult modified = placementHandler.onUseBlock(client, hitResult);

                if (modified != null) {
                    // Store it for the mixin to use
                    FlexiblePlacementHandler.setModifiedHitResult(modified);
                }
            }

            return InteractionResult.PASS; // Always pass - let vanilla handle it
        });
    }
}