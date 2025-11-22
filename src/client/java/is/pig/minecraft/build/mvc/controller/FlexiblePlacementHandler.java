package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FlexiblePlacementHandler {

    // Recursion guard for the fake click
    private boolean isPlacing = false;

    /**
     * Called every tick to update the visual state (PlacementSession).
     */
    public void onTick(Minecraft client) {
        boolean isFlexDown = InputController.flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        if (isFlexDown) {
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                // Update the model with the current calculated offset
                Direction offset = PlacementCalculator.getOffsetDirection(hit);
                PlacementSession.getInstance().setCurrentOffset(offset);
            } else {
                PlacementSession.getInstance().setCurrentOffset(null);
            }
        }
    }

    /**
     * Handles the Right-Click event to perform the custom placement.
     */
    public InteractionResult onUseBlock(Minecraft client, InteractionHand hand, BlockHitResult hitResult) {
        // A. Recursion Guard: If this is our own fake click, let it pass
        if (this.isPlacing) {
            return InteractionResult.PASS;
        }

        // B. Basic Checks
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        
        // Direct key check is more reliable for immediate input than session state
        if (!InputController.flexibleKey.isDown()) return InteractionResult.PASS;

        // C. Instant Calculation
        // We do not rely on the session state (updated in tick) for the action, 
        // we recalculate exactly where the player is looking NOW.
        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);

        // If aiming at center -> Standard Vanilla Behavior
        if (offset == null) {
            return InteractionResult.PASS;
        }

        // D. Flexible Placement Logic (Edge detected)
        if (client.level != null && client.level.isClientSide) {
            
            // Anti-Spam: If vanilla cooldown is active, consume event without doing anything
            if (((MinecraftAccessorMixin) client).getRightClickDelay() > 0) {
                return InteractionResult.SUCCESS;
            }

            // Create the Fake Hit Result (Same pos, but forcing the Offset Face)
            BlockHitResult newHit = new BlockHitResult(
                hitResult.getLocation(),
                offset,
                hitResult.getBlockPos(),
                hitResult.isInside()
            );

            try {
                this.isPlacing = true; // Raise guard

                // Force delay to 0 to ensure our click is accepted
                ((MinecraftAccessorMixin) client).setRightClickDelay(0);

                // Perform the action
                InteractionResult result = client.gameMode.useItemOn(client.player, hand, newHit);

                if (result.consumesAction()) {
                    if (result.shouldSwing()) client.player.swing(hand);
                    
                    // Success: Set cooldown to prevent double placement next frame
                    ((MinecraftAccessorMixin) client).setRightClickDelay(4);
                    
                    return InteractionResult.SUCCESS;
                }
            } finally {
                this.isPlacing = false; // Lower guard
            }

            // Fail safe: If placement failed (e.g. obstructed), still block vanilla placement
            // to avoid placing a block on the original face.
            ((MinecraftAccessorMixin) client).setRightClickDelay(4);
            return InteractionResult.FAIL;
        }

        return InteractionResult.PASS;
    }
}