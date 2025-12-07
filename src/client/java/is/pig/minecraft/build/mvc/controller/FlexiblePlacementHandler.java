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

    private boolean wasFlexibleKeyDown = false;
    private boolean wasAdjacentKeyDown = false;

    public void onTick(Minecraft client) {
        // Determine active mode
        PlacementMode mode = getActiveMode();
        boolean isActive = mode != PlacementMode.VANILLA;

        PlacementSession session = PlacementSession.getInstance();
        session.setActive(isActive);

        // Track key state changes for locking
        boolean flexibleDown = InputController.flexibleKey.isDown();
        boolean adjacentDown = InputController.adjacentKey.isDown();

        // Unlock when keys are released
        if (!flexibleDown && wasFlexibleKeyDown) {
            PiggyBuildClient.LOGGER.info(
                    "[FlexiblePlacement] Unlocking - flexible key released (was locked: " + session.isLocked() + ")");
            session.unlock();
        }
        if (!adjacentDown && wasAdjacentKeyDown) {
            PiggyBuildClient.LOGGER.info(
                    "[FlexiblePlacement] Unlocking - adjacent key released (was locked: " + session.isLocked() + ")");
            session.unlock();
        }

        wasFlexibleKeyDown = flexibleDown;
        wasAdjacentKeyDown = adjacentDown;

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
        if (InputController.flexibleKey.isDown()) {
            return PlacementMode.FLEXIBLE;
        } else if (InputController.adjacentKey.isDown()) {
            return PlacementMode.ADJACENT;
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

        PlacementSession session = PlacementSession.getInstance();

        PiggyBuildClient.LOGGER.info("[Handler] Modifying hit result for " + mode + " placement");
        PiggyBuildClient.LOGGER
                .info("[Handler] Session locked: " + session.isLocked() + ", Locked mode: " + session.getLockedMode());

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
            PiggyBuildClient.LOGGER.info("[Handler] Using LOCKED offset: " + offset + ", face: " + clickedFace
                    + " from last placed pos: " + pos);
        } else {
            // Calculate new offset and face, then lock them for subsequent placements
            offset = PlacementCalculator.getOffsetDirection(hitResult);
            clickedFace = hitResult.getDirection();
            pos = hitResult.getBlockPos();
            session.lock(offset, clickedFace, mode);
            PiggyBuildClient.LOGGER.info("[Handler] LOCKING NEW offset: " + offset + ", face: " + clickedFace
                    + " for mode: " + mode + " at pos: " + pos);
        }

        // Create a modified hit result with the locked face if needed
        BlockHitResult workingHitResult = hitResult;
        if (session.isLocked() && clickedFace != hitResult.getDirection()) {
            // Use the locked face instead of the current cursor face
            workingHitResult = BlockPlacer.createHitResult(hitResult.getBlockPos(), clickedFace);
            PiggyBuildClient.LOGGER.info("[Handler] Overriding hit result face from " + hitResult.getDirection()
                    + " to locked face: " + clickedFace);
        }

        BlockHitResult result;
        if (mode == PlacementMode.FLEXIBLE) {
            result = handleFlexibleMode(workingHitResult, pos, offset);
        } else if (mode == PlacementMode.ADJACENT) {
            result = handleAdjacentMode(workingHitResult, pos, offset);
        } else {
            result = hitResult;
        }

        // Store the target position for next placement
        if (result != null) {
            session.setLastPlacedPos(result.getBlockPos());
            PiggyBuildClient.LOGGER.info("[Handler] Stored last placed pos: " + result.getBlockPos());
        }

        return result;
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

        PiggyBuildClient.LOGGER.info("[Handler] Adjacent Mode - Original pos: " + pos);
        PiggyBuildClient.LOGGER.info("[Handler] Adjacent Mode - Clicked face: " + clickedFace);
        PiggyBuildClient.LOGGER.info("[Handler] Adjacent Mode - Offset: " + offset);

        if (offset == null) {
            // CENTER -> Place TWO blocks away (skipping one)
            // Move TWO positions in the clicked direction
            BlockPos skippedPos = pos.relative(clickedFace, 2);
            Direction placementFace = clickedFace.getOpposite();

            PiggyBuildClient.LOGGER.info("[Handler] Adjacent - STRAIGHT (SKIP ONE)");
            PiggyBuildClient.LOGGER.info("[Handler] Adjacent - Original: " + pos);
            PiggyBuildClient.LOGGER.info("[Handler] Adjacent - Skipped to: " + skippedPos);
            PiggyBuildClient.LOGGER.info("[Handler] Adjacent - Placement face: " + placementFace);

            return BlockPlacer.createHitResult(skippedPos, placementFace);
        } else {
            // EDGE -> Place diagonally adjacent (combine clicked face + edge direction)
            // First move in the clicked face direction, then in the edge direction
            BlockPos diagonalPos = pos.relative(clickedFace).relative(offset);

            // The placement face should be opposite to the edge direction
            // This creates a nice diagonal/stair pattern
            Direction placementFace = offset.getOpposite();

            PiggyBuildClient.LOGGER
                    .info("[Handler] Adjacent - DIAGONAL (Face " + placementFace + " at " + diagonalPos + ")");
            return BlockPlacer.createHitResult(diagonalPos, placementFace);
        }
    }
}