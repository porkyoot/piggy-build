package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class FlexiblePlacementHandler {

    // Store the modified hit result for the mixin to use
    private static ThreadLocal<BlockHitResult> modifiedHitResult = new ThreadLocal<>();

    public void onTick(Minecraft client) {
        // Visuals and State only
        boolean isFlexDown = InputController.flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        if (isFlexDown && client.hitResult instanceof BlockHitResult hit) {
            Direction offset = PlacementCalculator.getOffsetDirection(hit);
            PlacementSession.getInstance().setCurrentOffset(offset);
        } else {
            PlacementSession.getInstance().setCurrentOffset(null);
        }
    }

    public BlockHitResult onUseBlock(Minecraft client, BlockHitResult hitResult) {
        
        // 1. CLIENT-SIDE ONLY
        if (!client.isSameThread()) {
            return null; // Don't modify on server thread
        }

        // 2. BASIC FILTERS
        if (!InputController.flexibleKey.isDown()) {
            return null; // No modification needed
        }

        PiggyBuildClient.LOGGER.info("[Handler] Intercepted Flexible Click!");

        // 3. CALCULATE TARGET
        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);
        BlockPos pos = hitResult.getBlockPos();
        BlockHitResult newHit;

        if (offset == null) {
            // --- CASE A: CENTER -> PLACE BEHIND ---
            Direction currentFace = hitResult.getDirection();
            Direction oppositeFace = currentFace.getOpposite();
            
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 hitPos = center.add(
                oppositeFace.getStepX() * 0.5, 
                oppositeFace.getStepY() * 0.5, 
                oppositeFace.getStepZ() * 0.5
            );
            
            newHit = new BlockHitResult(hitPos, oppositeFace, pos, hitResult.isInside());
            PiggyBuildClient.LOGGER.info("[Handler] Target: BEHIND (Face " + oppositeFace + ")");
        } else {
            // --- CASE B: EDGE -> PLACE OFFSET ---
            Vec3 center = Vec3.atCenterOf(pos);
            Vec3 hitPos = center.add(
                offset.getStepX() * 0.5, 
                offset.getStepY() * 0.5, 
                offset.getStepZ() * 0.5
            );
            
            newHit = new BlockHitResult(hitPos, offset, pos, hitResult.isInside());
            PiggyBuildClient.LOGGER.info("[Handler] Target: OFFSET (Face " + offset + ")");
        }

        // Return the modified hit result
        return newHit;
    }

    // Static method for mixin to check if modification is active
    public static BlockHitResult getModifiedHitResult() {
        return modifiedHitResult.get();
    }

    public static void setModifiedHitResult(BlockHitResult hit) {
        modifiedHitResult.set(hit);
    }

    public static void clearModifiedHitResult() {
        modifiedHitResult.remove();
    }
}