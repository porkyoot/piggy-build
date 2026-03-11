package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.mvc.model.BuildShape;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.lib.ui.AntiCheatFeedbackManager;
import is.pig.minecraft.lib.ui.BlockReason;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.lib.placement.BlockPlacer;

import java.util.LinkedList;
import java.util.Queue;

public class ShapePlacementHandler {

    private static final float EPSILON = 0.002f;
    
    private static final Queue<BlockPos> placementQueue = new LinkedList<>();
    private static long lastPlacementTime = 0;

    public static void onTick(Minecraft client) {
        if (placementQueue.isEmpty() || client.player == null) return;
        
        int cps = PiggyBuildConfig.getInstance().getTickDelay();
        long minDelay = cps > 0 ? 1000L / cps : 0;
        
        long currentTime = System.currentTimeMillis();
        
        if (minDelay == 0) {
            // Uncapped
            while (!placementQueue.isEmpty()) {
                BlockPos pos = placementQueue.poll();
                placeAt(client, InteractionHand.MAIN_HAND, pos);
            }
            lastPlacementTime = currentTime;
        } else {
            // Rate Limited
            if (currentTime - lastPlacementTime >= minDelay) {
                int blocksToPlace = (int) ((currentTime - lastPlacementTime) / minDelay);
                if (blocksToPlace > placementQueue.size()) {
                    blocksToPlace = placementQueue.size();
                }
                
                for (int i = 0; i < blocksToPlace; i++) {
                    placeAt(client, InteractionHand.MAIN_HAND, placementQueue.poll());
                }
                
                lastPlacementTime += blocksToPlace * minDelay;
                if (lastPlacementTime > currentTime) {
                    lastPlacementTime = currentTime;
                }
            }
        }
    }

    public static InteractionResult tryPlaceShape(Minecraft client, InteractionHand hand) {
        BuildSession session = BuildSession.getInstance();
        if (!session.isActive()) {
            return null; // Not active, let vanilla handle it
        }

        net.minecraft.world.item.ItemStack stackInHand = client.player.getItemInHand(hand);
        if (!(stackInHand.getItem() instanceof net.minecraft.world.item.BlockItem)) {
            return null; // Standard placement/interaction handles non-blocks
        }

        BlockPos anchor = session.getAnchorPos();
        Axis axis = session.getAnchorAxis();
        double radius = session.getRadius();
        BuildShape shape = session.getShape();

        if (shape == BuildShape.BLOCK) {
            return null; // Standard placement handles this
        }

        if (!client.player.isShiftKeyDown()) {
            net.minecraft.world.level.block.state.BlockState hitState = client.level.getBlockState(anchor);
            if (hitState.hasBlockEntity()) {
                return null; // Let vanilla open the chest/container
            }
            if (hitState.getBlock() instanceof net.minecraft.world.level.block.CraftingTableBlock ||
                hitState.getBlock() instanceof net.minecraft.world.level.block.AnvilBlock ||
                hitState.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock ||
                hitState.getBlock() instanceof net.minecraft.world.level.block.DoorBlock ||
                hitState.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock ||
                hitState.getBlock() instanceof net.minecraft.world.level.block.ButtonBlock ||
                hitState.getBlock() instanceof net.minecraft.world.level.block.LeverBlock) {
                return null; // Let vanilla interact with basic functional blocks
            }
        }

        if (!PiggyBuildConfig.getInstance().isFeatureShapeBuilderEnabled()) {
            AntiCheatFeedbackManager.getInstance().onFeatureBlocked("shape_builder", BlockReason.SERVER_ENFORCEMENT);
            return InteractionResult.PASS;
        }

        int initialSize = placementQueue.size();

        if (shape == BuildShape.LINE) {
            queueLine(client, anchor, axis, radius);
        } else if (shape == BuildShape.RING) {
            queueRing(client, anchor, axis, radius);
        } else if (shape == BuildShape.SPHERE) {
            queueSphere(client, anchor, radius);
        }

        if (placementQueue.size() > initialSize) {
            if (initialSize == 0) {
                lastPlacementTime = System.currentTimeMillis();
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static void queueIfValid(Minecraft client, BlockPos anchor, BlockPos targetPos) {
        if (!targetPos.equals(anchor)) {
            placementQueue.add(targetPos);
        }
    }

    private static void queueLine(Minecraft client, BlockPos anchor, Axis axis, double radius) {
        int len = (int) Math.ceil(radius);

        for (int i = -len; i <= len; i++) {
            BlockPos targetPos = switch (axis) {
                case X -> anchor.offset(i, 0, 0);
                case Y -> anchor.offset(0, i, 0);
                case Z -> anchor.offset(0, 0, i);
            };
            queueIfValid(client, anchor, targetPos);
        }
    }

    private static void queueRing(Minecraft client, BlockPos anchor, Axis axis, double radius) {
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
                        case Y -> anchor.offset(u, 0, v);
                        case Z -> anchor.offset(u, v, 0);
                        case X -> anchor.offset(0, v, u);
                    };

                    queueIfValid(client, anchor, targetPos);
                }
            }
        }
    }

    private static void queueSphere(Minecraft client, BlockPos anchor, double radius) {
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
                        BlockPos targetPos = anchor.offset(x, y, z);
                        queueIfValid(client, anchor, targetPos);
                    }
                }
            }
        }
    }

    private static boolean placeAt(Minecraft client, InteractionHand hand, BlockPos targetPos) {
        if (client.level == null || !client.level.getBlockState(targetPos).canBeReplaced()) {
            return false;
        }

        // Just use the simplest placement: center face UP for prediction/packet.
        // BlockPlacer.placeBlock takes care of generating the correct packet.
        return BlockPlacer.placeBlock(targetPos, Direction.UP, hand);
    }
}
