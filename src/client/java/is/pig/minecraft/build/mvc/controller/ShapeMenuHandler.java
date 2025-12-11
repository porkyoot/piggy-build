package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.lib.ui.GenericRadialMenuScreen; // Imported from lib
import is.pig.minecraft.build.mvc.model.BuildSession; 
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

    public boolean onScroll(double amount) {
        if (InputController.triggerKey.isDown()) {
            BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
            return true; 
        }
        return false; 
    }

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
            
            BuildSession.getInstance().setAnchor(hit.getBlockPos(), hit.getDirection().getAxis());
            
            BuildShape center = BuildShape.BLOCK;
            List<BuildShape> radials = Arrays.stream(BuildShape.values())
                    .filter(s -> s != center)
                    .collect(Collectors.toList());

            client.setScreen(new GenericRadialMenuScreen<>(
                Component.literal("Build Menu"),
                center,
                radials,
                BuildSession.getInstance().getShape(),
                KeyBindingHelper.getBoundKeyOf(InputController.triggerKey),
                (newShape) -> BuildSession.getInstance().setShape(newShape),
                () -> {}, 
                (shape) -> {
                    if (shape == BuildShape.BLOCK) return null;
                    int r = (int) BuildSession.getInstance().getRadius();
                    return Component.literal(String.valueOf(r));
                },
                (amount) -> {
                    BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
                    return true;
                },
                null, null
            ));
        } else {
            BuildSession.getInstance().clearAnchor();
        }
    }
}