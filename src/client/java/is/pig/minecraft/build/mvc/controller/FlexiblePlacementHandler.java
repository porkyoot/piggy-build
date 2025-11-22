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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class FlexiblePlacementHandler {

    private boolean isPlacing = false;

    public void onTick(Minecraft client) {
        // 1. Visual Feedback Only (Logic is now handled in onUseBlock)
        boolean isFlexDown = InputController.flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        if (isFlexDown && client.hitResult instanceof BlockHitResult hit) {
            Direction offset = PlacementCalculator.getOffsetDirection(hit);
            PlacementSession.getInstance().setCurrentOffset(offset);
        } else {
            PlacementSession.getInstance().setCurrentOffset(null);
        }
    }

    public InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        
        // 1. Server & Basic Checks
        if (!world.isClientSide) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        // 2. RECURSION GUARD (The Secret Sauce)
        // When we call mc.gameMode.useItemOn below, it triggers this event again.
        // We must return PASS here to let that "inner" call proceed.
        if (this.isPlacing) {
            return InteractionResult.PASS;
        }

        // 3. Key Check
        if (!InputController.flexibleKey.isDown()) return InteractionResult.PASS;

        PiggyBuildClient.LOGGER.info("[Handler] Flexible Click Intercepted.");

        // 4. Calculate Target
        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);
        BlockPos pos = hitResult.getBlockPos();
        BlockHitResult newHit;

        // Logic from your first file (Handling both Offset and Behind)
        if (offset == null) {
            // CENTER CASE -> Place Behind (Opposite Face)
            Direction currentFace = hitResult.getDirection();
            Direction oppositeFace = currentFace.getOpposite();
            
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 hitPos = center.add(
                oppositeFace.getStepX() * 0.51, 
                oppositeFace.getStepY() * 0.51, 
                oppositeFace.getStepZ() * 0.51
            );
            newHit = new BlockHitResult(hitPos, oppositeFace, pos, hitResult.isInside());
            PiggyBuildClient.LOGGER.info("[Handler] Action: BEHIND");
        } else {
            // EDGE CASE -> Offset Placement
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 hitPos = center.add(
                offset.getStepX() * 0.51, 
                offset.getStepY() * 0.51, 
                offset.getStepZ() * 0.51
            );
            newHit = new BlockHitResult(hitPos, offset, pos, hitResult.isInside());
            PiggyBuildClient.LOGGER.info("[Handler] Action: OFFSET");
        }

        // 5. EXECUTION
        Minecraft mc = Minecraft.getInstance();
        
        try {
            // Flag start to handle the recursion loop
            this.isPlacing = true;
            
            // Kill delay for instant feel
            ((MinecraftAccessorMixin) mc).setRightClickDelay(0);

            // FIRE THE EVENT (Synchronously)
            // This will re-trigger onUseBlock, hit the 'isPlacing' check above, returns PASS, 
            // and then execute the packet sending inside useItemOn.
            InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, newHit);

            PiggyBuildClient.LOGGER.info("[Handler] Inner Result: " + result);

            if (result.consumesAction()) {
                if (result.shouldSwing()) mc.player.swing(hand);
                
                // Reset delay
                ((MinecraftAccessorMixin) mc).setRightClickDelay(4);
                
                // CRITICAL: Return SUCCESS here to block the ORIGINAL vanilla click
                return InteractionResult.SUCCESS;
            }
            
        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("Placement Error", e);
        } finally {
            // Always reset state
            this.isPlacing = false;
        }

        // 6. FAILURE FALLBACK
        // If our custom logic failed (e.g. block couldn't be placed), we still return FAIL.
        // If we returned PASS here, Vanilla would try to place the block normally on the original face.
        ((MinecraftAccessorMixin) mc).setRightClickDelay(4);
        return InteractionResult.FAIL;
    }
}