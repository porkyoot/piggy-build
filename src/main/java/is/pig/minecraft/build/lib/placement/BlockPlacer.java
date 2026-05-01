package is.pig.minecraft.build.lib.placement;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.world.InteractBlockAction;

public class BlockPlacer {

    public static boolean placeBlock(BlockPos pos, Direction face, InteractionHand hand) {
        return placeBlock(pos, face, hand, false);
    }

    public static boolean placeBlock(BlockPos pos, Direction face, InteractionHand hand, boolean ignoreGlobalCps) {
        BlockHitResult hitResult = createHitResult(pos, face);
        return placeBlock(hitResult, hand, ignoreGlobalCps);
    }

    public static boolean placeBlock(BlockHitResult hitResult, InteractionHand hand) {
        return placeBlock(hitResult, hand, false);
    }

    public static boolean placeBlock(BlockHitResult hitResult, InteractionHand hand, boolean ignoreGlobalCps) {
        try {
            Action action = createAction(hitResult, hand, ignoreGlobalCps);
            if (action != null) {
                PiggyActionQueue.getInstance().enqueue(action);
                // Trigger inventory refill via registry or other decoupled way if possible
                // For now, keeping it simple as per request
                return true;
            }
        } catch (Exception e) {
            // Log error via API if available
            return false;
        }
        return false;
    }

    public static Action createAction(BlockHitResult hitResult, InteractionHand hand, boolean ignoreGlobalCps) {
        var action = new InteractBlockAction(hitResult, hand, "piggy-build");
        if (ignoreGlobalCps) action.setIgnoreGlobalCps(true);
        return action;
    }

    public static BlockHitResult createHitResult(BlockPos pos, Direction face) {
        Vec3 center = new Vec3(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        Vec3 hitPos = new Vec3(
                center.x() + face.getX() * 0.5,
                center.y() + face.getY() * 0.5,
                center.z() + face.getZ() * 0.5);

        return new BlockHitResult(hitPos, face, pos, false);
    }

    public static BlockHitResult createHitResult(BlockPos pos, Direction face, double u, double v) {
        Vec3 hitPos = calculateHitPosition(pos, face, u, v);
        return new BlockHitResult(hitPos, face, pos, false);
    }

    private static Vec3 calculateHitPosition(BlockPos pos, Direction face, double u, double v) {
        double x = pos.x();
        double y = pos.y();
        double z = pos.z();

        return switch (face) {
            case DOWN -> new Vec3(x + u, y, z + v);
            case UP -> new Vec3(x + u, y + 1.0, z + v);
            case NORTH -> new Vec3(x + u, y + v, z);
            case SOUTH -> new Vec3(x + u, y + v, z + 1.0);
            case WEST -> new Vec3(x, y + v, z + u);
            case EAST -> new Vec3(x + 1.0, y + v, z + u);
        };
    }
}
