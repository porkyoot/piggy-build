package is.pig.minecraft.build.lib.placement;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Utility class for placing blocks at any position/face.
 * Handles all the client-side tricks: packet sending, prediction, swing
 * animation, delays, etc.
 */
public class BlockPlacer {

    /**
     * Place a block at the specified position and face.
     * This is the main entry point - handles everything automatically.
     * 
     * @param pos  The block position to interact with
     * @param face The face to place on
     * @param hand Which hand to use (usually MAIN_HAND)
     * @return true if placement was successful
     */
    public static boolean placeBlock(BlockPos pos, Direction face, InteractionHand hand) {
        return placeBlock(pos, face, hand, false);
    }

    public static boolean placeBlock(BlockPos pos, Direction face, InteractionHand hand, boolean ignoreGlobalCps) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) {
            return false;
        }

        // Create a BlockHitResult for this position and face
        BlockHitResult hitResult = createHitResult(pos, face);

        return placeBlock(hitResult, hand, ignoreGlobalCps);
    }

    /**
     * Place a block using a pre-constructed BlockHitResult.
     * This gives more control over the exact hit position.
     * 
     * @param hitResult The block hit result (position, face, hit location)
     * @param hand      Which hand to use
     * @return true if placement was successful
     */
    public static boolean placeBlock(BlockHitResult hitResult, InteractionHand hand) {
        return placeBlock(hitResult, hand, false);
    }

    public static boolean placeBlock(BlockHitResult hitResult, InteractionHand hand, boolean ignoreGlobalCps) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) {
            return false;
        }

        try {
            ItemStack itemStack = mc.player.getItemInHand(hand);
            boolean isWaterBucket = itemStack.is(net.minecraft.world.item.Items.WATER_BUCKET);
            BlockPos targetPos = hitResult.getBlockPos().relative(hitResult.getDirection());
            
            java.util.function.BooleanSupplier verifyCondition = () -> {
                if (mc.level == null) return false;
                net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(targetPos);
                return isWaterBucket ? state.is(net.minecraft.world.level.block.Blocks.WATER) : !state.isAir();
            };

            ((MinecraftAccessorMixin) mc).setRightClickDelay(0);
            var action = new is.pig.minecraft.lib.action.world.InteractBlockAction(hitResult, hand, "piggy-build", verifyCondition);
            if (ignoreGlobalCps) action.setIgnoreGlobalCps(true);
            is.pig.minecraft.lib.action.PiggyActionQueue.getInstance().enqueue(action);
            ((MinecraftAccessorMixin) mc).setRightClickDelay(4);
            
            triggerInventoryRefill(mc);
            return true;
        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[BlockPlacer] Placement using action failed", e);
            return false;
        }
    }

    /**
     * Cross-mod compatibility: Try to notify piggy-inventory's AutoRefillHandler
     * to immediately refill blocks if the hand just became empty, bypassing game tick delays.
     */
    private static void triggerInventoryRefill(Minecraft mc) {
        try {
            Class<?> clazz = Class.forName("is.pig.minecraft.inventory.handler.AutoRefillHandler");
            Object instance = clazz.getMethod("getInstance").invoke(null);
            clazz.getMethod("onTick", Minecraft.class).invoke(instance, mc);
        } catch (Exception e) {
            // Piggy Inventory not installed, ignore.
        }
    }

    /**
     * Create a BlockHitResult for a given position and face.
     * Uses the center of the target face as the hit position.
     */
    public static BlockHitResult createHitResult(BlockPos pos, Direction face) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 hitPos = center.add(
                face.getStepX() * 0.5,
                face.getStepY() * 0.5,
                face.getStepZ() * 0.5);

        return new BlockHitResult(hitPos, face, pos, false);
    }

    /**
     * Create a BlockHitResult with a custom hit position on the face.
     * 
     * @param pos  Block position
     * @param face Face to interact with
     * @param u    U coordinate on face (0.0 to 1.0)
     * @param v    V coordinate on face (0.0 to 1.0)
     */
    public static BlockHitResult createHitResult(BlockPos pos, Direction face, double u, double v) {
        Vec3 hitPos = calculateHitPosition(pos, face, u, v);
        return new BlockHitResult(hitPos, face, pos, false);
    }

    /**
     * Calculate the exact world position for a UV coordinate on a block face.
     */
    private static Vec3 calculateHitPosition(BlockPos pos, Direction face, double u, double v) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        switch (face) {
            case DOWN: // -Y face
                return new Vec3(x + u, y, z + v);
            case UP: // +Y face
                return new Vec3(x + u, y + 1.0, z + v);
            case NORTH: // -Z face
                return new Vec3(x + u, y + v, z);
            case SOUTH: // +Z face
                return new Vec3(x + u, y + v, z + 1.0);
            case WEST: // -X face
                return new Vec3(x, y + v, z + u);
            case EAST: // +X face
                return new Vec3(x + 1.0, y + v, z + u);
            default:
                return Vec3.atCenterOf(pos);
        }
    }

    /**
     * Check if a block can be placed at the given position.
     * This checks basic conditions but doesn't validate game rules.
     */
    public static boolean canPlaceBlock(BlockPos pos, Direction face) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return false;
        }

        // Check if player has a block item
        ItemStack itemStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            return false;
        }

        // Check if the target position is loaded
        if (!mc.level.isLoaded(pos)) {
            return false;
        }

        // Add more checks as needed (e.g., reach distance, creative mode, etc.)

        return true;
    }
}