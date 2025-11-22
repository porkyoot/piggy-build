package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class FlexiblePlacementHandler {

    private static final Logger LOGGER = PiggyBuildClient.LOGGER;
    private boolean isPlacing = false;

    public void onTick(Minecraft client) {
        boolean isFlexDown = InputController.flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        if (isFlexDown) {
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                Direction offset = PlacementCalculator.getOffsetDirection(hit);
                PlacementSession.getInstance().setCurrentOffset(offset);
            } else {
                PlacementSession.getInstance().setCurrentOffset(null);
            }
        }
    }

    public InteractionResult onUseBlock(Minecraft client, InteractionHand hand, BlockHitResult hitResult) {
        // 1. IMPORTANT: Do not modify server-side!
        // UseBlockCallback is invoked on both sides. We only act on the client
        // to send a modified placement packet.
        if (client.level != null && !client.level.isClientSide) {
            return InteractionResult.PASS;
        }

        // 2. Recursion protection
        if (this.isPlacing) return InteractionResult.PASS;

        // 3. Basic checks
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!InputController.flexibleKey.isDown()) return InteractionResult.PASS;

        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);
        if (offset == null) return InteractionResult.PASS; // Centre -> Vanilla

        // --- FORCED PLACEMENT LOGIC ---
        
        // Log the delay for information, but do not stop on it
        // LOGGER.info("Delay before forcing: " + ((MinecraftAccessorMixin) client).getRightClickDelay());

        BlockPos pos = hitResult.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos);
        
        // Precise calculation of the impact point on the new face (offset)
        Vec3 newHitPos = center.add(
            offset.getStepX() * 0.5,
            offset.getStepY() * 0.5,
            offset.getStepZ() * 0.5
        );

        BlockHitResult newHit = new BlockHitResult(newHitPos, offset, pos, hitResult.isInside());

        try {
            this.isPlacing = true;
            

            // FIX: Override the delay to force the action.
            ((MinecraftAccessorMixin) client).setRightClickDelay(0);

            // Execute the action with our fake click
            InteractionResult result = client.gameMode.useItemOn(client.player, hand, newHit);

            if (result.consumesAction()) {
                if (result.shouldSwing()) client.player.swing(hand);
                
                // Success: restore delay to 4 to avoid spam AFTER our action
                ((MinecraftAccessorMixin) client).setRightClickDelay(4);
                
                return InteractionResult.SUCCESS; // Stop Vanilla
            }
            
        } catch (Exception e) {
            LOGGER.error("Flexible placement error", e);
        } finally {
            this.isPlacing = false;
        }

        // If it failed (e.g. block in the way), restore the delay and
        // return FAIL to prevent the vanilla click on the original face.
        ((MinecraftAccessorMixin) client).setRightClickDelay(4);
        return InteractionResult.FAIL; 
    }
}