package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.build.lib.placement.PlacementMode;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

public class FlexiblePlacementHandler {

    public void onTick(Minecraft client) {
        // Determine active mode
        PlacementMode mode = getActiveMode();
        boolean isActive = mode != PlacementMode.VANILLA;
        
        PlacementSession.getInstance().setActive(isActive);

        if (isActive && client.hitResult instanceof BlockHitResult hit) {
            Direction offset = PlacementCalculator.getOffsetDirection(hit);
            PlacementSession.getInstance().setCurrentOffset(offset);
        } else {
            PlacementSession.getInstance().setCurrentOffset(null);
        }
    }

    /**
     * Determine which placement mode is currently active based on key states
     */
    public PlacementMode getActiveMode() {
        if (InputController.flexibleKey.isDown()) {
            return PlacementMode.FLEXIBLE;
        } else if (InputController.adjacentKey.isDown()) {
            return PlacementMode.ADJACENT;
        }
        return PlacementMode.VANILLA;
    }

    /**
     * Called from MinecraftClientMixin to modify the hit result before vanilla processes it.
     * Returns a modified BlockHitResult based on the active placement mode.
     */
    public BlockHitResult modifyHitResult(Minecraft client, BlockHitResult hitResult) {
        
        // 1. CLIENT-SIDE ONLY
        if (!client.isSameThread()) {
            return hitResult;
        }

        // 2. CHECK MODE
        PlacementMode mode = getActiveMode();
        if (mode == PlacementMode.VANILLA) {
            return hitResult;
        }

        PiggyBuildClient.LOGGER.info("[Handler] Modifying hit result for " + mode + " placement");

        // 3. CALCULATE TARGET BASED ON MODE
        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);
        BlockPos pos = hitResult.getBlockPos();
        
        if (mode == PlacementMode.FLEXIBLE) {
            return handleFlexibleMode(hitResult, pos, offset);
        } else if (mode == PlacementMode.ADJACENT) {
            return handleAdjacentMode(hitResult, pos, offset);
        }

        return hitResult;
    }

    /**
     * FLEXIBLE MODE: Place on edge faces or behind center
     */
    private BlockHitResult handleFlexibleMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction targetFace;

        if (offset == null) {
            // CENTER -> PLACE BEHIND (opposite face)
            targetFace = hitResult.getDirection().getOpposite();
            PiggyBuildClient.LOGGER.info("[Handler] Flexible - BEHIND (Face " + targetFace + ")");
        } else {
            // EDGE -> PLACE ON OFFSET FACE
            targetFace = offset;
            PiggyBuildClient.LOGGER.info("[Handler] Flexible - OFFSET (Face " + targetFace + ")");
        }

        return BlockPlacer.createHitResult(pos, targetFace);
    }

    /**
     * ADJACENT MODE: Place next to the block (diagonal or straight)
     * This creates stair/diagonal patterns
     */
    private BlockHitResult handleAdjacentMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction clickedFace = hitResult.getDirection();
        
        if (offset == null) {
            // CENTER -> Place adjacent on the SAME face (straight out)
            BlockPos adjacentPos = pos.relative(clickedFace);
            PiggyBuildClient.LOGGER.info("[Handler] Adjacent - STRAIGHT (Same face at " + adjacentPos + ")");
            return BlockPlacer.createHitResult(adjacentPos, clickedFace.getOpposite());
        } else {
            // EDGE -> Place diagonally adjacent (combine clicked face + edge direction)
            // First move in the clicked face direction, then in the edge direction
            BlockPos diagonalPos = pos.relative(clickedFace).relative(offset);
            
            // The placement face should be opposite to the edge direction
            // This creates a nice diagonal/stair pattern
            Direction placementFace = offset.getOpposite();
            
            PiggyBuildClient.LOGGER.info("[Handler] Adjacent - DIAGONAL (Face " + placementFace + " at " + diagonalPos + ")");
            return BlockPlacer.createHitResult(diagonalPos, placementFace);
        }
    }
}