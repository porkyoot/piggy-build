package is.pig.minecraft.build.lib.math;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import is.pig.minecraft.build.PiggyBuildClient;

public class PlacementCalculator {

    private static final Logger LOGGER = PiggyBuildClient.LOGGER;
    private static final double CENTER_MARGIN = 0.25;

    private static Direction lastResult = null;
    private static long lastLogTime = 0;

    public static Direction getOffsetDirection(BlockHitResult hit) {
        Direction face = hit.getDirection();
        Vec3 hitPos = hit.getLocation();
        BlockPos p = hit.getBlockPos();

        // 1. Calculate local coordinates
        double x = hitPos.x - p.getX();
        double y = hitPos.y - p.getY();
        double z = hitPos.z - p.getZ();

        // 2. Calculate UV
        double[] uv = getFaceUV(p, hitPos, face);
        double u = uv[0];
        double v = uv[1];

        // 3. Calculate distances
        double distTop = v;
        double distBottom = 1.0 - v;
        double distLeft = u;
        double distRight = 1.0 - u;

        double min = Math.min(Math.min(distTop, distBottom), Math.min(distLeft, distRight));

        // 4. Determine result
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

        // --- LOGGING FIXED ---
        long now = System.currentTimeMillis();
        // Log when the result changes or every 2 seconds to avoid spamming
        if (result != lastResult || (now - lastLogTime > 2000)) {
            
            // NOTE: Use String.format for neat number formatting
            String logMessage = String.format(
                "Math Debug -> Face: %s | Local(x:%.2f, y:%.2f, z:%.2f) => UV(u:%.2f, v:%.2f) => Result: %s", 
                face, x, y, z, u, v, (result == null ? "center" : result)
            );
            
            LOGGER.info(logMessage);
            
            lastResult = result;
            lastLogTime = now;
        }

        return result;
    }

    // --- HELPERS (unchanged) ---

    public static float getTextureRotation(Direction face, Direction offset) {
        if (getDirectionFromRotation(face, 0) == offset) return 0;
        if (getDirectionFromRotation(face, 180) == offset) return 180;
        if (getDirectionFromRotation(face, 90) == offset) return -90;
        if (getDirectionFromRotation(face, -90) == offset) return 90;
        return 0;
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
        };
    }

    private static double[] getFaceUV(BlockPos pos, Vec3 hit, Direction face) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();

        return switch (face) {
            case UP -> new double[]{x, z};     
            case DOWN -> new double[]{x, 1-z}; 
            case NORTH -> new double[]{1-x, 1-y}; 
            case SOUTH -> new double[]{x, 1-y};
            case WEST -> new double[]{z, 1-y};
            case EAST -> new double[]{1-z, 1-y};
        };
    }
}