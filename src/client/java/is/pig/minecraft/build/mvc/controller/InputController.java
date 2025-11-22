package is.pig.minecraft.build.mvc.controller;

import com.mojang.blaze3d.platform.InputConstants;
import is.pig.minecraft.build.lib.event.MouseScrollCallback;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.lib.ui.GenericRadialMenuScreen;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.BuildShape;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InputController {

    public static KeyMapping triggerKey;
    public static KeyMapping flexibleKey;
    private boolean wasKeyDown = false;

    // State for Debug Toast to avoid spam
    private Direction lastFace = null;
    private Direction lastOffset = null;
    private boolean wasFlexActive = false;

    public void initialize() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Shape Selector", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "Piggy Build"
        ));
        flexibleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Flexible Placement",
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_4, // Bouton Souris 4
            "Piggy Build"
        ));

        MouseScrollCallback.EVENT.register((amount) -> {
            if (triggerKey.isDown()) {
                BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
                return true;
            }
            return false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client.player == null) return;

        boolean isKeyDown = triggerKey.isDown();
        boolean isFlexDown = flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        // DÉTECTION DU CLIC (Just Pressed)
        if (isKeyDown && !wasKeyDown && client.screen == null) {
            handleTriggerPress(client);
        }

        if (isFlexDown) {
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                
                Direction currentFace = hit.getDirection();
                Direction offset = PlacementCalculator.getOffsetDirection(hit);
                
                PlacementSession.getInstance().setCurrentOffset(offset);

                // --- DEBUG TOAST LOGIC ---
                boolean stateChanged = (currentFace != lastFace) || (offset != lastOffset);
                
                if (stateChanged) {
                    String offsetText = (offset == null) ? "CENTER" : offset.name();
                    
                    // CORRECTION ICI : SystemToast.Id (et non SystemToastIds)
                    SystemToast.add(
                        client.getToasts(),
                        SystemToastId.PERIODIC_NOTIFICATION, // <--- CHANGEMENT ICI
                        Component.literal("Face: " + currentFace.name()),
                        Component.literal("Direction: " + offsetText)
                    );

                    lastFace = currentFace;
                    lastOffset = offset;
                }
            } else {
                PlacementSession.getInstance().setCurrentOffset(null);
            }
        } else {
            lastFace = null;
            lastOffset = null;
        }
    }

    private void handleTriggerPress(Minecraft client) {
        // 1. VÉRIFICATION : Est-ce qu'on regarde un bloc valide ?
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK) {
            
            // CAS DU VIDE : On nettoie tout !
            // Le rendu se désactive car l'anchorPos devient null
            BuildSession.getInstance().clearAnchor();
            
            // ET ON S'ARRÊTE LÀ (Pas de menu)
            return; 
        }

        // 2. CAS VALIDE : On met à jour l'ancre
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BuildSession.getInstance().setAnchor(hit.getBlockPos(), hit.getDirection().getAxis());

        // 3. Préparation des données pour le menu
        BuildShape center = BuildShape.BLOCK;
        List<BuildShape> radials = Arrays.stream(BuildShape.values())
                .filter(s -> s != center)
                .collect(Collectors.toList());

        // 4. Ouverture du Menu
        client.setScreen(new GenericRadialMenuScreen<>(
            Component.literal("Build Menu"),
            center,
            radials,
            BuildSession.getInstance().getShape(),
            KeyBindingHelper.getBoundKeyOf(triggerKey),
            (newShape) -> BuildSession.getInstance().setShape(newShape),
            () -> {}, // Close callback
            
            // Texte du Rayon
            (shape) -> {
                if (shape == BuildShape.BLOCK) return null;
                int r = (int) BuildSession.getInstance().getRadius();
                return Component.literal(String.valueOf(r));
            },

            // Scroll Callback
            (amount) -> {
                modifyRadius(amount);
                return true;
            }
        ));
    }

    private void modifyRadius(double amount) {
        BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
    }
}