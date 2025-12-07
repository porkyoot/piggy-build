package is.pig.minecraft.build.lib.placement;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
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
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) {
            return false;
        }

        // Create a BlockHitResult for this position and face
        BlockHitResult hitResult = createHitResult(pos, face);

        return placeBlock(hitResult, hand);
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
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        MultiPlayerGameMode gameMode = mc.gameMode;

        if (player == null || gameMode == null) {
            return false;
        }

        // Method 1: Try the clean approach - let the game handle everything
        boolean success = placeUsingGameMode(mc, player, gameMode, hitResult, hand);

        if (success) {
            // PiggyBuildClient.LOGGER.info("[BlockPlacer] Placement successful via
            // GameMode");
            return true;
        }

        // Method 2: If that fails, use direct packet + prediction
        success = placeUsingPacket(mc, player, gameMode, hitResult, hand);

        if (success) {
            // PiggyBuildClient.LOGGER.info("[BlockPlacer] Placement successful via
            // Packet");
            return true;
        }

        PiggyBuildClient.LOGGER.warn("[BlockPlacer] Placement failed");
        return false;
    }

    /**
     * Method 1: Use the standard GameMode.useItemOn approach.
     * This is the cleanest and most compatible method.
     */
    private static boolean placeUsingGameMode(Minecraft mc, LocalPlayer player,
            MultiPlayerGameMode gameMode,
            BlockHitResult hitResult,
            InteractionHand hand) {
        try {
            // Clear any click delay to allow immediate placement
            ((MinecraftAccessorMixin) mc).setRightClickDelay(0);

            // Use the standard placement method
            InteractionResult result = gameMode.useItemOn(player, hand, hitResult);

            // Handle the result
            if (result.consumesAction()) {
                // Swing animation
                if (result.shouldSwing()) {
                    player.swing(hand);
                }

                // Restore normal delay
                ((MinecraftAccessorMixin) mc).setRightClickDelay(4);

                return true;
            }

            // Restore delay even on failure
            ((MinecraftAccessorMixin) mc).setRightClickDelay(4);

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[BlockPlacer] GameMode method failed", e);
        }

        return false;
    }

    /**
     * Method 2: Direct packet sending + client prediction.
     * More manual control, used as fallback.
     */
    private static boolean placeUsingPacket(Minecraft mc, LocalPlayer player,
            MultiPlayerGameMode gameMode,
            BlockHitResult hitResult,
            InteractionHand hand) {
        try {
            ClientPacketListener connection = mc.getConnection();
            if (connection == null) {
                return false;
            }

            // Send the packet to server
            ServerboundUseItemOnPacket packet = new ServerboundUseItemOnPacket(hand, hitResult, 0);
            connection.send(packet);

            // Do client-side prediction
            ItemStack itemStack = player.getItemInHand(hand);
            Level level = player.level();

            if (!itemStack.isEmpty() && level != null) {
                UseOnContext context = new UseOnContext(player, hand, hitResult);
                InteractionResult result = itemStack.useOn(context);

                if (result.consumesAction()) {
                    if (result.shouldSwing()) {
                        player.swing(hand);
                    }
                    return true;
                }
            }

        } catch (Exception e) {
            PiggyBuildClient.LOGGER.error("[BlockPlacer] Packet method failed", e);
        }

        return false;
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