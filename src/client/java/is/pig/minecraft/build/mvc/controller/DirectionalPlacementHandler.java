package is.pig.minecraft.build.mvc.controller;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.lib.placement.PlacementCalculator;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.lib.placement.PlacementMode;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import is.pig.minecraft.lib.ui.AntiCheatFeedbackManager;
import is.pig.minecraft.lib.ui.BlockReason;

public class DirectionalPlacementHandler {

    private boolean wasDirectionalKeyDown = false;
    private boolean wasDiagonalKeyDown = false;
    private boolean hasShownFeedback = false;

    public void onTick(Object client) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        boolean directionalDown = input.isKeyDown("piggy-build:directional");
        boolean diagonalDown = input.isKeyDown("piggy-build:diagonal");

        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        boolean isEnabled = config.isFeatureFlexiblePlacementEnabled();

        if ((directionalDown || diagonalDown) && !isEnabled) {
            PlacementSession.getInstance().setActive(false);
            if (!hasShownFeedback) {
                boolean serverForces = !config.serverAllowCheats
                        || (config.serverFeatures != null && config.serverFeatures.containsKey("flexible_placement")
                                && !Boolean.TRUE.equals(config.serverFeatures.get("flexible_placement")));
                BlockReason reason = serverForces ? BlockReason.SERVER_ENFORCEMENT : BlockReason.LOCAL_CONFIG;
                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("flexible_placement", reason);
                hasShownFeedback = true;
            }
            wasDirectionalKeyDown = directionalDown;
            wasDiagonalKeyDown = diagonalDown;
            return;
        }

        if (!directionalDown && !diagonalDown) {
            hasShownFeedback = false;
        }

        PlacementMode mode = getActiveMode();
        boolean isActive = (mode != PlacementMode.VANILLA) && isEnabled;

        PlacementSession session = PlacementSession.getInstance();
        session.setActive(isActive);

        if (!directionalDown && wasDirectionalKeyDown) {
            session.unlock();
        }
        if (!diagonalDown && wasDiagonalKeyDown) {
            session.unlock();
        }

        wasDirectionalKeyDown = directionalDown;
        wasDiagonalKeyDown = diagonalDown;

        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        HitResult hitResult = worldState.getCrosshairTarget(client);

        if (isActive && hitResult instanceof BlockHitResult hit) {
            Direction offset = PlacementCalculator.getOffsetDirection(hit);
            session.setCurrentOffset(offset);
        } else {
            session.setCurrentOffset(null);
        }
    }

    public PlacementMode getActiveMode() {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        if (input.isKeyDown("piggy-build:directional")) {
            return PlacementMode.DIRECTIONAL;
        } else if (input.isKeyDown("piggy-build:diagonal")) {
            return PlacementMode.DIAGONAL;
        }
        return PlacementMode.VANILLA;
    }

    public BlockHitResult modifyHitResult(Object client, BlockHitResult hitResult) {
        PlacementMode mode = getActiveMode();
        if (mode == PlacementMode.VANILLA)
            return hitResult;

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
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            if (worldState.isReplaceable(worldState.getCurrentWorldId(), pos)) {
                return null;
            }
            offset = session.getLockedOffset();
            clickedFace = session.getLockedFace();
        } else {
            offset = PlacementCalculator.getOffsetDirection(hitResult);
            clickedFace = hitResult.side();
            pos = hitResult.blockPos();
            session.lock(offset, clickedFace, mode);
        }

        BlockHitResult workingHitResult = hitResult;
        if (session.isLocked() && clickedFace != hitResult.side()) {
            workingHitResult = BlockPlacer.createHitResult(hitResult.blockPos(), clickedFace);
        }

        BlockHitResult result;
        if (mode == PlacementMode.DIRECTIONAL) {
            result = handleDirectionalMode(workingHitResult, pos, offset);
        } else if (mode == PlacementMode.DIAGONAL) {
            result = handleDiagonalMode(workingHitResult, pos, offset);
        } else {
            result = hitResult;
        }

        if (result != null && session.isLocked()) {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            String worldId = worldState.getCurrentWorldId();
            BlockPos targetPlacementPos = getExpectedPlacementPos(mode, pos, clickedFace, offset);
            int maxIter = 100;
            while (maxIter-- > 0 && !worldState.isReplaceable(worldId, targetPlacementPos)) {
                pos = targetPlacementPos;
                session.setLastPlacedPos(pos);

                if (mode == PlacementMode.DIRECTIONAL) {
                    result = handleDirectionalMode(workingHitResult, pos, offset);
                } else if (mode == PlacementMode.DIAGONAL) {
                    result = handleDiagonalMode(workingHitResult, pos, offset);
                }
                targetPlacementPos = getExpectedPlacementPos(mode, pos, clickedFace, offset);
            }
        }

        if (result != null) {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            double reach = worldState.getPlayerReachDistance(client);
            if (worldState.getPlayerEyePosition(client).distanceToSqr(result.hitVec()) > reach * reach) {
                return null;
            }
            BlockPos expectedPos = getExpectedPlacementPos(mode, pos, clickedFace, offset);
            session.setPendingLastPlacedPos(expectedPos);
        }

        return result;
    }

    public void onBlockPlaced(BlockHitResult result) {
        PlacementSession session = PlacementSession.getInstance();
        if (!session.isLocked() || session.getPendingLastPlacedPos() == null) {
            return;
        }
        session.setLastPlacedPos(session.getPendingLastPlacedPos());
    }

    private BlockHitResult handleDirectionalMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction targetFace = (offset == null) ? hitResult.side().getOpposite() : offset;
        return BlockPlacer.createHitResult(pos, targetFace);
    }

    private BlockHitResult handleDiagonalMode(BlockHitResult hitResult, BlockPos pos, Direction offset) {
        Direction clickedFace = hitResult.side();
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

    private BlockPos getExpectedPlacementPos(PlacementMode mode, BlockPos pos, Direction clickedFace, Direction offset) {
        if (mode == PlacementMode.DIRECTIONAL) {
            Direction targetFace = (offset == null) ? clickedFace.getOpposite() : offset;
            return pos.relative(targetFace);
        } else if (mode == PlacementMode.DIAGONAL) {
            if (offset == null) {
                return pos.relative(clickedFace, 2);
            } else {
                return pos.relative(clickedFace).relative(offset);
            }
        }
        return pos.relative(clickedFace);
    }
}