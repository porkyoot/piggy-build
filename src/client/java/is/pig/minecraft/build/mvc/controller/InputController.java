package is.pig.minecraft.build.mvc.controller;

import com.mojang.blaze3d.platform.InputConstants;
import is.pig.minecraft.build.lib.ui.GenericRadialMenuScreen;
import is.pig.minecraft.build.lib.event.MouseScrollCallback;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.BuildShape;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InputController {

    public static KeyMapping triggerKey;
    private boolean wasKeyDown = false;

    public void initialize() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "Shape Selector", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.piggy_build"
        ));

        // Mixin Scroll (In-Game)
        MouseScrollCallback.EVENT.register((amount) -> {
            if (triggerKey.isDown()) {
                modifyRadius(amount);
                return true;
            }
            return false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft client) {
        if (client.player == null) return;
        boolean isKeyDown = triggerKey.isDown();

        if (isKeyDown && !wasKeyDown && client.screen == null) {
            handleTriggerPress(client);
        }
        wasKeyDown = isKeyDown;
    }

    private void handleTriggerPress(Minecraft client) {
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) client.hitResult;
            BuildSession.getInstance().setAnchor(hit.getBlockPos(), hit.getDirection().getAxis());
        }

        BuildShape center = BuildShape.BLOCK;
        List<BuildShape> radials = Arrays.stream(BuildShape.values())
                .filter(s -> s != center)
                .collect(Collectors.toList());

        client.setScreen(new GenericRadialMenuScreen<>(
            Component.literal("Build Menu"),
            center,
            radials,
            BuildSession.getInstance().getShape(),
            KeyBindingHelper.getBoundKeyOf(triggerKey),
            (newShape) -> BuildSession.getInstance().setShape(newShape),
            () -> {},
            
            // Provider pour le texte du rayon
            (shape) -> {
                if (shape == BuildShape.BLOCK) return null;
                int r = (int) BuildSession.getInstance().getRadius();
                return Component.literal(String.valueOf(r));
            },

            // NOUVEAU : Callback pour le scroll DANS le menu
            (amount) -> {
                modifyRadius(amount);
                return true; // Scroll géré
            }
        ));
    }

    private void modifyRadius(double amount) {
        BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
        // Optionnel : Feedback chat (mais tu as demandé de l'enlever, donc je ne le mets pas)
    }
}