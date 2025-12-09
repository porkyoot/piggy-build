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

public class DirectionalPlacementHandler {

    private boolean wasDirectionalKeyDown = false;
    private boolean wasDiagonalKeyDown = false;

    public void onTick(Minecraft client) {
        // Determine active mode
        PlacementMode mode = getActiveMode();
        boolean isActive = mode != PlacementMode.VANILLA;

        PlacementSession session = PlacementSession.getInstance();
        session.setActive(isActive);

        // Track key state changes for locking
        boolean directionalDown = InputController.directionalKey.isDown();
        boolean diagonalDown = InputController.diagonalKey.isDown();

        // Unlock when keys are released
        if (!directionalDown && wasDirectionalKeyDown) {
            PiggyBuildClient.LOGGER.debug(
                    "[DirectionalPlacement] Unlocking - directional key released (was locked: " + session.isLocked()
                            + ")");
            session.unlock();
        }
        if (!diagonalDown && wasDiagonalKeyDown) {
            PiggyBuildClient.LOGGER.debug(
                    "[DirectionalPlacement] Unlocking - diagonal key released (was locked: " + session.isLocked()
                            + ")");
            session.unlock();
        }

        wasDirectionalKeyDown = directionalDown;
        wasDiagonalKeyDown = diagonalDown;

        if (isActive && client.hitResult instanceof BlockHitResult hit) {
            Direction offset = PlacementCalculator.getOffsetDirection(hit);
            session.setCurrentOffset(offset);
        } else {
            session.setCurrentOffset(null);
        }
    }

    /**
     * Determine which placement mode is currently active based on key states
     */
    public PlacementMode getActiveMode() {
        if (InputController.directionalKey.isDown()) {
            return PlacementMode.DIRECTIONAL;
        } else if (InputController.diagonalKey.isDown()) {
            return PlacementMode.DIAGONAL;
        }
        return PlacementMode.VANILLA;
    }

    /**
     * Called from MinecraftClientMixin to modify the hit result before vanilla
     * processes it.
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

        // 3. CHECK IF FEATURE IS ENABLED
        is.pig.minecraft.build.config.PiggyConfig config = is.pig.minecraft.build.config.PiggyConfig.getInstance();
        if (!config.isFeatureFlexiblePlacementEnabled()) {
            // Feature is disabled, return vanilla hit result
            return hitResult;
        }

        PlacementSession session = PlacementSession.getInstance();

        PiggyBuildClient.LOGGER.debug("[Handler] Modifying hit result for " + mode + " placement");
        PiggyBuildClient.LOGGER
                .debug("[Handler] Session locked: " + session.isLocked() + ", Locked mode: " + session.getLockedMode());

        // 3. DETERMINE OFFSET, FACE, AND POSITION
        Direction offset;
        Direction clickedFace;
        BlockPos pos;

        if (session.isLocked() && session.getLockedMode() == mode && session.getLastPlacedPos() != null) {
            // Use the locked offset, face, and calculate next position from last placed
            // block
            offset = session.getLockedOffset();
            clickedFace = session.getLockedFace();
            pos = session.getLastPlacedPos();
            PiggyBuildClient.LOGGER.debug("[Handler] Using LOCKED offset: " + offset + ", face: " + clickedFace
                    + " from last placed pos: " + pos);
        } else {
            // Calculate new offset and face, then lock them for subsequent placements
            offset = PlacementCalculator.getOffsetDirection(hitResult);
            clickedFace = hitResult.getDirection();
            pos = hitResult.getBlockPos();
            session.lock(offset, clickedFace, mode);
            PiggyBuildClient.LOGGER.debug("[Handler] LOCKING NEW offset: " + offset + ", face: " + clickedFace
                    + " for mode: " + mode + " at pos: " + pos);
        }

        // Create a modified hit result with the locked face if needed
        BlockHitResult workingHitResult = hitResult;
        if (session.isLocked() && clickedFace != hitResult.getDirection()) {
            // Use the locked face instead of the current cursor face
            workingHitResult = BlockPlacer.createHitResult(hitResult.getBlockPos(), clickedFace);
            PiggyBuildClient.LOGGER.debug("[Handler] Overriding hit result face from " + hitResult.getDirection()
                    + " to locked face: " + clickedFace);
        }

        BlockHitResult result;
        if (mode == PlacementMode.DIRECTIONAL) {
            result = handleDirectionalMode(workingHitResult, pos, offset);
        } else if (mode == PlacementMode.DIAGONAL) {
            result = handleDiagonalMode(workingHitResult, pos, offset);
        } else {
            result = hitResult;
        }

        // Store the target position for next placement
        if (result != null) {
            // For correct chaining:
            // 1. If we clicked ON a block (Directional/Vanilla), the NEW block is at
            // pos.relative(face)
            // 2. If we targeted a neighbor (Diagonal), the NEW block is implicitly there or
            // relative to it

            BlockPos nextPos = result.getBlockPos();

            // If the hit result targets the same position as the input (common in
            // Directional
            // mode),
            // and we're placing on a face, the next block will be at that face offset.
            if (nextPos.equals(pos)) {
                nextPos = nextPos.relative(result.getDirection());
            }

            session.setLastPlacedPos(nextPos);
            PiggyBuildClient.LOGGER.debug("[Handler] Stored last placed pos: " + nextPos + " (derived from result: "
                    + result.getBlockPos() + ")");
        }

        return result;
    }

    /**
     * DIRECTIONAL MODE: Place on edge faces or behind center
     */
    private BlockHitResult handleDirectionalMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction targetFace;

        if (offset == null) {
            // CENTER -> PLACE BEHIND (opposite face)
            targetFace = hitResult.getDirection().getOpposite();
            PiggyBuildClient.LOGGER.debug("[Handler] Directional - BEHIND (Face " + targetFace + ")");
        } else {
            // EDGE -> PLACE ON OFFSET FACE
            targetFace = offset;
            PiggyBuildClient.LOGGER.debug("[Handler] Directional - OFFSET (Face " + targetFace + ")");
        }

        return BlockPlacer.createHitResult(pos, targetFace);
    }

    /**
     * DIAGONAL MODE: Place next to the block (diagonal or straight)
     * This creates stair/diagonal patterns
     */
    private BlockHitResult handleDiagonalMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction clickedFace = hitResult.getDirection();

        PiggyBuildClient.LOGGER.debug("[Handler] Diagonal Mode - Original pos: " + pos);
        PiggyBuildClient.LOGGER.debug("[Handler] Diagonal Mode - Clicked face: " + clickedFace);
        PiggyBuildClient.LOGGER.debug("[Handler] Diagonal Mode - Offset: " + offset);

        if (offset == null) {
            // CENTER -> Place TWO blocks away (skipping one)
            // Move TWO positions in the clicked direction
            BlockPos skippedPos = pos.relative(clickedFace, 2);
            Direction placementFace = clickedFace.getOpposite();

            PiggyBuildClient.LOGGER.debug("[Handler] Diagonal - STRAIGHT (SKIP ONE)");
            PiggyBuildClient.LOGGER.debug("[Handler] Diagonal - Original: " + pos);
            PiggyBuildClient.LOGGER.debug("[Handler] Diagonal - Skipped to: " + skippedPos);
            PiggyBuildClient.LOGGER.debug("[Handler] Diagonal - Placement face: " + placementFace);

            return BlockPlacer.createHitResult(skippedPos, placementFace);
        } else {
            // EDGE -> Place diagonally adjacent (combine clicked face + edge direction)
            // First move in the clicked face direction, then in the edge direction
            BlockPos diagonalPos = pos.relative(clickedFace).relative(offset);

            // The placement face should be opposite to the edge direction
            // This creates a nice diagonal/stair pattern
            Direction placementFace = offset.getOpposite();

            PiggyBuildClient.LOGGER
                    .debug("[Handler] Diagonal - DIAGONAL (Face " + placementFace + " at " + diagonalPos + ")");
            return BlockPlacer.createHitResult(diagonalPos, placementFace);
        }
    }
}