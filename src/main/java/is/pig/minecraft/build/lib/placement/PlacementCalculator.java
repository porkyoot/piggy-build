package is.pig.minecraft.build.lib.placement;

import is.pig.minecraft.api.*;

public class PlacementCalculator {

    private static final double CENTER_MARGIN = 0.25;

    public static Direction getOffsetDirection(BlockHitResult hit) {
        Direction face = hit.direction();
        Vec3 hitPos = hit.pos();
        BlockPos p = hit.blockPos();

        // 1. Calculate UV
        double[] uv = getFaceUV(p, hitPos, face);
        double u = uv[0];
        double v = uv[1];

        // 2. Calculate distances
        double distTop = v;
        double distBottom = 1.0 - v;
        double distLeft = u;
        double distRight = 1.0 - u;

        double min = Math.min(Math.min(distTop, distBottom), Math.min(distLeft, distRight));

        // 3. Determine result
        Direction result = null; // Default: CENTER

        if (min > CENTER_MARGIN) {
            result = null;
        } else if (min == distTop) {
            result = getDirectionFromRotation(face, 0);
        } else if (min == distBottom) {
            result = getDirectionFromRotation(face, 180);
        } else if (min == distRight) {
            result = getDirectionFromRotation(face, 90);
        } else {
            result = getDirectionFromRotation(face, -90);
        }

        return result;
    }

    private static Direction getDirectionFromRotation(Direction face, int angle) {
        return switch (face) {
            case UP -> switch (angle) {
                case 0 -> Direction.NORTH;
                case 180 -> Direction.SOUTH;
                case 90 -> Direction.EAST;
                case -90 -> Direction.WEST;
                default -> Direction.UP;
            };
            case DOWN -> switch (angle) {
                case 0 -> Direction.SOUTH;
                case 180 -> Direction.NORTH;
                case 90 -> Direction.EAST;
                case -90 -> Direction.WEST;
                default -> Direction.DOWN;
            };
            case NORTH -> switch (angle) {
                case 0 -> Direction.UP;
                case 180 -> Direction.DOWN;
                case 90 -> Direction.WEST;
                case -90 -> Direction.EAST;
                default -> Direction.NORTH;
            };
            case SOUTH -> switch (angle) {
                case 0 -> Direction.UP;
                case 180 -> Direction.DOWN;
                case 90 -> Direction.EAST;
                case -90 -> Direction.WEST;
                default -> Direction.SOUTH;
            };
            case WEST -> switch (angle) {
                case 0 -> Direction.UP;
                case 180 -> Direction.DOWN;
                case 90 -> Direction.SOUTH;
                case -90 -> Direction.NORTH;
                default -> Direction.WEST;
            };
            case EAST -> switch (angle) {
                case 0 -> Direction.UP;
                case 180 -> Direction.DOWN;
                case 90 -> Direction.NORTH;
                case -90 -> Direction.SOUTH;
                default -> Direction.EAST;
            };
            default -> face;
        };
    }

    private static double[] getFaceUV(BlockPos pos, Vec3 hit, Direction face) {
        double x = hit.x() - pos.x();
        double y = hit.y() - pos.y();
        double z = hit.z() - pos.z();

        return switch (face) {
            case UP -> new double[] { x, z };
            case DOWN -> new double[] { x, 1 - z };
            case NORTH -> new double[] { 1 - x, 1 - y };
            case SOUTH -> new double[] { x, 1 - y };
            case WEST -> new double[] { z, 1 - y };
            case EAST -> new double[] { 1 - z, 1 - y };
            default -> new double[] { 0.5, 0.5 };
        };
    }

    public static float getTextureRotation(Direction face, Direction offset) {
        if (offset == null) return 0;

        return switch (face) {
            case UP -> switch (offset) {
                case NORTH -> 0;
                case SOUTH -> 180;
                case EAST -> 90;
                case WEST -> -90;
                default -> 0;
            };
            case DOWN -> switch (offset) {
                case SOUTH -> 0;
                case NORTH -> 180;
                case EAST -> 90;
                case WEST -> -90;
                default -> 0;
            };
            case NORTH -> switch (offset) {
                case UP -> 0;
                case DOWN -> 180;
                case WEST -> 90;
                case EAST -> -90;
                default -> 0;
            };
            case SOUTH -> switch (offset) {
                case UP -> 0;
                case DOWN -> 180;
                case EAST -> 90;
                case WEST -> -90;
                default -> 0;
            };
            case WEST -> switch (offset) {
                case UP -> 0;
                case DOWN -> 180;
                case SOUTH -> 90;
                case NORTH -> -90;
                default -> 0;
            };
            case EAST -> switch (offset) {
                case UP -> 0;
                case DOWN -> 180;
                case NORTH -> 90;
                case SOUTH -> -90;
                default -> 0;
            };
            default -> 0;
        };
    }
}
