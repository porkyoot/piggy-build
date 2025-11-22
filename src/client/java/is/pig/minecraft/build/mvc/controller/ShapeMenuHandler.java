package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.lib.ui.GenericRadialMenuScreen;
import is.pig.minecraft.build.mvc.model.BuildSession; // <--- On utilise le Model
import is.pig.minecraft.build.mvc.model.BuildShape;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ShapeMenuHandler {

    private boolean wasKeyDown = false;

    /**
     * Handles scroll wheel to change radius.
     */
    public boolean onScroll(double amount) {
        if (InputController.triggerKey.isDown()) {
            // CORRECTION : Appel direct au Model (BuildSession)
            BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
            return true; 
        }
        return false; 
    }

    /**
     * Handles ticking logic (Detecting key press to open menu).
     */
    public void onTick(Minecraft client) {
        boolean isTriggerDown = InputController.triggerKey.isDown();

        if (isTriggerDown && !wasKeyDown && client.screen == null) {
            handleMenuTrigger(client);
        }

        wasKeyDown = isTriggerDown;
    }

    private void handleMenuTrigger(Minecraft client) {
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) client.hitResult;
            
            // Update Model: Set Anchor
            BuildSession.getInstance().setAnchor(hit.getBlockPos(), hit.getDirection().getAxis());
            
            // Prepare View Data
            BuildShape center = BuildShape.BLOCK;
            List<BuildShape> radials = Arrays.stream(BuildShape.values())
                    .filter(s -> s != center)
                    .collect(Collectors.toList());

            // Open View (Menu)
            client.setScreen(new GenericRadialMenuScreen<>(
                Component.literal("Build Menu"),
                center,
                radials,
                
                // CORRECTION : On lit la forme depuis la Session
                BuildSession.getInstance().getShape(),
                
                KeyBindingHelper.getBoundKeyOf(InputController.triggerKey),
                
                // CORRECTION : On écrit la nouvelle forme dans la Session
                (newShape) -> BuildSession.getInstance().setShape(newShape),
                
                () -> {}, // Close callback
                
                // Extra Info Provider (Radius text)
                (shape) -> {
                    if (shape == BuildShape.BLOCK) return null;
                    // CORRECTION : On lit le rayon depuis la Session
                    int r = (int) BuildSession.getInstance().getRadius();
                    return Component.literal(String.valueOf(r));
                },
                
                // In-Menu Scroll Callback
                (amount) -> {
                    // CORRECTION : On modifie le rayon dans la Session
                    BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
                    return true;
                }
            ));
        } else {
            // Clear anchor
            BuildSession.getInstance().clearAnchor();
        }
    }
}