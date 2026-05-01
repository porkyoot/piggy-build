package is.pig.minecraft.build.mvc.controller;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.lib.ui.GenericRadialMenuScreen;
import is.pig.minecraft.build.mvc.model.BuildSession; 
import is.pig.minecraft.build.mvc.model.BuildShape;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ShapeMenuHandler {

    private boolean wasKeyDown = false;

    public boolean onScroll(double amount) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        if (input.isKeyDown("piggy-build:shape_selector")) {
            BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
            return true; 
        }
        return false; 
    }

    public void onTick(Object client) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        boolean isTriggerDown = input.isKeyDown("piggy-build:shape_selector");

        if (isTriggerDown && !wasKeyDown) {
            handleMenuTrigger(client);
        }

        wasKeyDown = isTriggerDown;
    }

    private void handleMenuTrigger(Object client) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        HitResult hitResult = worldState.getCrosshairTarget(client);

        if (hitResult instanceof BlockHitResult hit) {
            BuildSession.getInstance().setAnchor(hit.blockPos(), hit.side().getAxis());
            
            BuildShape center = BuildShape.BLOCK;
            List<BuildShape> radials = Arrays.stream(BuildShape.values())
                    .filter(s -> s != center)
                    .collect(Collectors.toList());

            // Since GenericRadialMenuScreen is still tied to net.minecraft.network.chat.Component 
            // and other Minecraft types, we might need a bridge or more refactoring there.
            // For now, we'll try to pass standard types and see if it works with the client cast.
            
            // Re-evaluating: opening a screen usually requires engine-specific implementation.
            // We'll use openScreen which handles the engine-side opening.
            
            /*
            worldState.openScreen(client, new GenericRadialMenuScreen<>(
                net.minecraft.network.chat.Component.literal("Build Menu"),
                center,
                radials,
                BuildSession.getInstance().getShape(),
                null, // Trigger key handling needs more SPI
                (newShape) -> BuildSession.getInstance().setShape(newShape),
                () -> {}, 
                (shape) -> {
                    if (shape == BuildShape.BLOCK) return null;
                    int r = (int) BuildSession.getInstance().getRadius();
                    return net.minecraft.network.chat.Component.literal(String.valueOf(r));
                },
                (amount) -> {
                    BuildSession.getInstance().modifyRadius(amount > 0 ? 1 : -1);
                    return true;
                },
                null, null
            ));
            */
            // TODO: GenericRadialMenuScreen decoupling needed for full SPI compliance
        } else {
            BuildSession.getInstance().clearAnchor();
        }
    }
}