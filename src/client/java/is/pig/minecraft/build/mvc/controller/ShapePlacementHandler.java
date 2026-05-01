package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.ItemDataAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mvc.model.BuildShape;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.lib.ui.AntiCheatFeedbackManager;
import is.pig.minecraft.lib.ui.BlockReason;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.lib.action.BulkAction;
import is.pig.minecraft.lib.action.PiggyActionQueue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ShapePlacementHandler {

    private static final float EPSILON = 0.002f;
    
    private static final Queue<BlockPos> placementQueue = new LinkedList<>();

    public static void onTick(Object client) {
    }

    public static InteractionResult tryPlaceShape(Object client, InteractionHand hand) {
        BuildSession session = BuildSession.getInstance();
        if (!session.isActive()) {
            return null;
        }

        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        ItemDataAdapter itemData = PiggyServiceRegistry.getItemDataAdapter();
        
        Object player = client;
        Object stackInHand = worldState.getPlayerMainHandItem(player);
        
        if (!itemData.isBlockItem(stackInHand)) {
            return null;
        }

        BlockPos anchor = session.getAnchorPos();
        Direction.Axis axis = session.getAnchorAxis();
        double radius = session.getRadius();
        BuildShape shape = session.getShape();

        if (shape == BuildShape.BLOCK) {
            return null;
        }

        if (!PiggyBuildConfig.getInstance().isFeatureShapeBuilderEnabled()) {
            AntiCheatFeedbackManager.getInstance().onFeatureBlocked("shape_builder", BlockReason.SERVER_ENFORCEMENT);
            return InteractionResult.PASS;
        }

        if (shape == BuildShape.LINE) {
            queueLine(anchor, axis, radius);
        } else if (shape == BuildShape.RING) {
            queueRing(anchor, axis, radius);
        } else if (shape == BuildShape.SPHERE) {
            queueSphere(anchor, radius);
        }

        if (!placementQueue.isEmpty()) {
            int cps = PiggyBuildConfig.getInstance().getTickDelay();
            boolean ignoreGlobalCps = (cps <= 0);
            
            List<Action> actions = new ArrayList<>();
            while (!placementQueue.isEmpty()) {
                BlockPos pos = placementQueue.poll();
                BlockHitResult hitResult = BlockPlacer.createHitResult(pos, Direction.UP);
                Action act = BlockPlacer.createAction(hitResult, InteractionHand.MAIN_HAND, ignoreGlobalCps);
                if (act != null) actions.add(act);
            }
            
            if (!actions.isEmpty()) {
                var bulkAction = new BulkAction(
                        "piggy-build",
                        ActionPriority.NORMAL,
                        actions,
                        () -> true,
                        0,
                        "Shape Placement"
                );
                if (ignoreGlobalCps) bulkAction.setIgnoreGlobalCps(true);
                PiggyActionQueue.getInstance().enqueue(bulkAction);
            }
            
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void queueIfValid(BlockPos anchor, BlockPos targetPos) {
        if (!targetPos.equals(anchor)) {
            placementQueue.add(targetPos);
        }
    }

    private static void queueLine(BlockPos anchor, Direction.Axis axis, double radius) {
        int len = (int) Math.ceil(radius);

        for (int i = -len; i <= len; i++) {
            BlockPos targetPos = switch (axis) {
                case X -> new BlockPos(anchor.x() + i, anchor.y(), anchor.z());
                case Y -> new BlockPos(anchor.x(), anchor.y() + i, anchor.z());
                case Z -> new BlockPos(anchor.x(), anchor.y(), anchor.z() + i);
            };
            queueIfValid(anchor, targetPos);
        }
    }

    private static void queueRing(BlockPos anchor, Direction.Axis axis, double radius) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int u = min; u <= max; u++) {
            for (int v = min; v <= max; v++) {
                double distSq = ((double) u * u) + ((double) v * v);

                if (distSq <= radiusSq && distSq > innerRadiusSq) {
                    BlockPos targetPos = switch (axis) {
                        case Y -> new BlockPos(anchor.x() + u, anchor.y(), anchor.z() + v);
                        case Z -> new BlockPos(anchor.x() + u, anchor.y() + v, anchor.z());
                        case X -> new BlockPos(anchor.x(), anchor.y() + v, anchor.z() + u);
                    };

                    queueIfValid(anchor, targetPos);
                }
            }
        }
    }

    private static void queueSphere(BlockPos anchor, double radius) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {
                    double distSq = ((double) x * x) + ((double) y * y) + ((double) z * z);

                    if (distSq <= radiusSq && distSq > innerRadiusSq) {
                        BlockPos targetPos = new BlockPos(anchor.x() + x, anchor.y() + y, anchor.z() + z);
                        queueIfValid(anchor, targetPos);
                    }
                }
            }
        }
    }
}
