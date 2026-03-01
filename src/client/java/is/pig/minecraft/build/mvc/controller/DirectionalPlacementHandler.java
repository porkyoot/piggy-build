package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.build.lib.placement.PlacementMode;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import is.pig.minecraft.lib.ui.AntiCheatFeedbackManager;
import is.pig.minecraft.lib.ui.BlockReason;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;

public class DirectionalPlacementHandler {

    private boolean wasDirectionalKeyDown = false;
    private boolean wasDiagonalKeyDown = false;
    private boolean hasShownFeedback = false;

    public void onTick(Minecraft client) {
        boolean directionalDown = InputController.directionalKey.isDown();
        boolean diagonalDown = InputController.diagonalKey.isDown();

        // Check permissions
        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        boolean isEnabled = config.isFeatureFlexiblePlacementEnabled();

        // 1. Handle Anti-Cheat Feedback logic
        // If user presses keys but feature is disabled, show feedback and reset session
        if ((directionalDown || diagonalDown) && !isEnabled) {
            PlacementSession.getInstance().setActive(false); // Hide overlay

            if (!hasShownFeedback) {
                // Determine reason (Client safety or Server enforced)
                boolean serverForces = !config.serverAllowCheats
                        || (config.serverFeatures != null && config.serverFeatures.containsKey("flexible_placement")
                                && !Boolean.TRUE.equals(config.serverFeatures.get("flexible_placement")));

                BlockReason reason = serverForces ? BlockReason.SERVER_ENFORCEMENT : BlockReason.LOCAL_CONFIG;

                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("flexible_placement", reason);
                hasShownFeedback = true; // Prevent spamming every tick
            }

            // Do not process further
            wasDirectionalKeyDown = directionalDown;
            wasDiagonalKeyDown = diagonalDown;
            return;
        }

        // Reset feedback flag when keys are released
        if (!directionalDown && !diagonalDown) {
            hasShownFeedback = false;
        }

        // 2. Normal Logic (only if enabled)
        PlacementMode mode = getActiveMode(); // Returns VANILLA if keys not down
        boolean isActive = (mode != PlacementMode.VANILLA) && isEnabled;

        PlacementSession session = PlacementSession.getInstance();
        session.setActive(isActive);

        // Unlock when keys are released
        if (!directionalDown && wasDirectionalKeyDown) {
            session.unlock();
        }
        if (!diagonalDown && wasDiagonalKeyDown) {
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
     * Called from MinecraftClientMixin to modify the hit result.
     */
    public BlockHitResult modifyHitResult(Minecraft client, BlockHitResult hitResult) {
        if (!client.isSameThread())
            return hitResult;

        PlacementMode mode = getActiveMode();
        if (mode == PlacementMode.VANILLA)
            return hitResult;

        // Double check config here, though onTick usually handles the state
        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        if (!config.isFeatureFlexiblePlacementEnabled()) {
            return hitResult;
        }

        PlacementSession session = PlacementSession.getInstance();

        Direction offset;
        Direction clickedFace;
        BlockPos pos;

        if (session.isLocked() && session.getLockedMode() == mode && session.getLastPlacedPos() != null) {
            pos = session.getLastPlacedPos();

            // Check if the previous block is STILL replaceable (i.e. not yet confirmed by
            // the server)
            if (client.level != null && client.level.getBlockState(pos).canBeReplaced()) {
                // Instead of returning null effectively canceling the sequence,
                // we return null to simply PAUSE the sequence until it's placed.
                return null;
            }

            offset = session.getLockedOffset();
            clickedFace = session.getLockedFace();
        } else {
            offset = PlacementCalculator.getOffsetDirection(hitResult);
            clickedFace = hitResult.getDirection();
            pos = hitResult.getBlockPos();
            session.lock(offset, clickedFace, mode);
        }

        BlockHitResult workingHitResult = hitResult;
        if (session.isLocked() && clickedFace != hitResult.getDirection()) {
            workingHitResult = BlockPlacer.createHitResult(hitResult.getBlockPos(), clickedFace);
        }

        BlockHitResult result;
        if (mode == PlacementMode.DIRECTIONAL) {
            result = handleDirectionalMode(workingHitResult, pos, offset);
        } else if (mode == PlacementMode.DIAGONAL) {
            result = handleDiagonalMode(workingHitResult, pos, offset);
        } else {
            result = hitResult;
        }

        if (result != null) {
            double reach = client.player != null ? client.player.blockInteractionRange() : 4.5;
            if (client.player != null
                    && client.player.getEyePosition().distanceToSqr(result.getLocation()) > reach * reach) {
                // Out of reach: do not update lastPlacedPos so we can try again next tick
                return null;
            }

            // Do NOT update lastPlacedPos yet. We must wait until the block is ACTUALLY
            // placed.
        }

        return result;
    }

    /**
     * Called when a block is successfully placed to advance the placement sequence
     * state.
     */
    public void onBlockPlaced(BlockHitResult result) {
        PlacementSession session = PlacementSession.getInstance();
        if (!session.isLocked()) {
            return; // State progression only matters if we are in a sequence
        }

        // When diagonal/directional creates a new hit result, getBlockPos returns the
        // block
        // the ray "hit". To find where the block was placed, we offset it by the
        // clicked face.
        BlockPos nextPos = result.getBlockPos();

        // If we clicked directly on the placed pos (like when looking away and building
        // a line),
        // we might need to offset it by the requested direction. In vanilla Minecraft,
        // the item
        // uses the clicked block, and places the block at
        // hitResult.getBlockPos().relative(hitResult.getDirection()).
        // In our modified hit result, getBlockPos() is ALREADY the target block where
        // we place it.
        // Or is it? BlockPlacer.createHitResult sets `pos` as the first argument, which
        // becomes getBlockPos().
        //
        // Wait, looking at modifyHitResult:
        // if (nextPos.equals(pos)) nextPos = nextPos.relative(result.getDirection());
        // session.setLastPlacedPos(nextPos);

        // Actually, if we just successfully placed a block, the new "lastPlacedPos" is
        // the block we just generated in our hit result AFTER we apply the face offset.
        nextPos = result.getBlockPos().relative(result.getDirection());
        session.setLastPlacedPos(nextPos);
    }

    private BlockHitResult handleDirectionalMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction targetFace;
        if (offset == null) {
            targetFace = hitResult.getDirection().getOpposite();
        } else {
            targetFace = offset;
        }
        return BlockPlacer.createHitResult(pos, targetFace);
    }

    private BlockHitResult handleDiagonalMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction clickedFace = hitResult.getDirection();
        if (offset == null) {
            BlockPos skippedPos = pos.relative(clickedFace, 2);
            Direction placementFace = clickedFace.getOpposite();
            return BlockPlacer.createHitResult(skippedPos, placementFace);
        } else {
            BlockPos diagonalPos = pos.relative(clickedFace).relative(offset);
            Direction placementFace = offset.getOpposite();
            return BlockPlacer.createHitResult(diagonalPos, placementFace);
        }
    }
}