package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class FlexiblePlacementHandler {

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

    /**
     * Called from MinecraftClientMixin to modify the hit result before vanilla processes it.
     * Returns a modified BlockHitResult based on where the player clicked.
     */
    public BlockHitResult modifyHitResult(Minecraft client, BlockHitResult hitResult) {
        
        // 1. CLIENT-SIDE ONLY
        if (!client.isSameThread()) {
            return hitResult;
        }

        // 2. BASIC FILTERS
        if (!InputController.flexibleKey.isDown()) {
            return hitResult;
        }

        PiggyBuildClient.LOGGER.info("[Handler] Modifying hit result for flexible placement");

        // 3. CALCULATE TARGET FACE
        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);
        BlockPos pos = hitResult.getBlockPos();
        Direction targetFace;

        if (offset == null) {
            // CENTER -> PLACE BEHIND (opposite face)
            targetFace = hitResult.getDirection().getOpposite();
            PiggyBuildClient.LOGGER.info("[Handler] Target: BEHIND (Face " + targetFace + ")");
        } else {
            // EDGE -> PLACE ON OFFSET FACE
            targetFace = offset;
            PiggyBuildClient.LOGGER.info("[Handler] Target: OFFSET (Face " + targetFace + ")");
        }

        // 4. CREATE NEW HIT RESULT using BlockPlacer utility
        return BlockPlacer.createHitResult(pos, targetFace);
    }
}