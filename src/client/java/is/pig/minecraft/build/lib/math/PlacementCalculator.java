package is.pig.minecraft.build.lib.math;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class PlacementCalculator {

    private static final double CENTER_THRESHOLD = 0.25; // 25% margin on each side

    /**
     * Calculates the direction of the neighbor block.
     * Returns NULL if the cursor is in the center of the face.
     */
    public static Direction getOffsetDirection(BlockHitResult hit) {
        Direction face = hit.getDirection();
        Vec3 hitPos = hit.getLocation();
        BlockPos blockPos = hit.getBlockPos();

        // Local coordinates [0, 1]
        double x = hitPos.x - blockPos.getX();
        double y = hitPos.y - blockPos.getY();
        double z = hitPos.z - blockPos.getZ();

        double dUp = 0, dDown = 0, dLeft = 0, dRight = 0;
        Direction up = null, down = null, left = null, right = null;

        switch (face) {
            case UP:
            case DOWN:
                dUp = z;            down = Direction.NORTH;
                dDown = 1 - z;      up = Direction.SOUTH;
                dLeft = x;          right = Direction.WEST;
                dRight = 1 - x;     left = Direction.EAST;
                break;

            case NORTH:
            case SOUTH:
                dUp = 1 - y;        up = Direction.UP;
                dDown = y;          down = Direction.DOWN;
                dLeft = x;          right = Direction.WEST;
                dRight = 1 - x;     left = Direction.EAST;
                break;

            case WEST:
            case EAST:
                dUp = 1 - y;        up = Direction.UP;
                dDown = y;          down = Direction.DOWN;
                dLeft = z;          right = Direction.NORTH;
                dRight = 1 - z;     left = Direction.SOUTH;
                break;
        }

        // Calculate minimum distance to any edge
        double min = Math.min(Math.min(dUp, dDown), Math.min(dLeft, dRight));

        // --- CENTER CHECK ---
        // If we are far enough from all edges, consider it CENTER.
        if (min > CENTER_THRESHOLD) {
            return null; // NULL means CENTER
        }

        if (min == dUp) return up;
        if (min == dDown) return down;
        if (min == dLeft) return left;
        return right;
    }
}