package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.lib.placement.BlockPlacer;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.BuildShape;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public class ShapePlacementHandler {

    private static final float EPSILON = 0.002f;

    public static InteractionResult tryPlaceShape(Minecraft client, InteractionHand hand) {
        BuildSession session = BuildSession.getInstance();
        if (!session.isActive()) {
            return null; // Not active, let vanilla handle it
        }

        BlockPos anchor = session.getAnchorPos();
        Axis axis = session.getAnchorAxis();
        double radius = session.getRadius();
        BuildShape shape = session.getShape();

        if (shape == BuildShape.BLOCK) {
            return null; // Standard placement handles this
        }

        // Just blindly try to place everywhere; BlockPlacer handles validation
        boolean anyPlaced = false;

        if (shape == BuildShape.LINE) {
            anyPlaced = placeLine(client, hand, anchor, axis, radius);
        } else if (shape == BuildShape.RING) {
            anyPlaced = placeRing(client, hand, anchor, axis, radius);
        } else if (shape == BuildShape.SPHERE) {
            anyPlaced = placeSphere(client, hand, anchor, radius);
        }

        if (anyPlaced) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private static boolean placeLine(Minecraft client, InteractionHand hand, BlockPos anchor, Axis axis,
            double radius) {
        boolean placed = false;
        int len = (int) Math.ceil(radius);

        for (int i = -len; i <= len; i++) {
            BlockPos targetPos = switch (axis) {
                case X -> anchor.offset(i, 0, 0);
                case Y -> anchor.offset(0, i, 0);
                case Z -> anchor.offset(0, 0, i);
            };
            if (!targetPos.equals(anchor) && placeAt(client, hand, targetPos)) {
                placed = true;
            }
        }
        return placed;
    }

    private static boolean placeRing(Minecraft client, InteractionHand hand, BlockPos anchor, Axis axis,
            double radius) {
        boolean placed = false;
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

                    if (!targetPos.equals(anchor) && placeAt(client, hand, targetPos)) {
                        placed = true;
                    }
                }
            }
        }
        return placed;
    }

    private static boolean placeSphere(Minecraft client, InteractionHand hand, BlockPos anchor, double radius) {
        boolean placed = false;
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
                        if (!targetPos.equals(anchor) && placeAt(client, hand, targetPos)) {
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static boolean placeAt(Minecraft client, InteractionHand hand, BlockPos targetPos) {
        if (!client.level.getBlockState(targetPos).canBeReplaced()) {
            return false;
        }

        // Just use the simplest placement: center face UP for prediction/packet.
        // BlockPlacer.placeBlock takes care of generating the correct packet.
        return BlockPlacer.placeBlock(targetPos, Direction.UP, hand);
    }
}
