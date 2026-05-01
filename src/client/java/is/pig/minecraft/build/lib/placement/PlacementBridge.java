package is.pig.minecraft.build.lib.placement;

import is.pig.minecraft.api.BlockHitResult;
import is.pig.minecraft.api.BlockPos;
import is.pig.minecraft.api.Direction;
import is.pig.minecraft.lib.util.TypeConverter;

/**
 * Bridge between Minecraft types and pure Java placement types.
 */
public class PlacementBridge {

    public static net.minecraft.world.phys.BlockHitResult toMinecraft(BlockHitResult hitResult) {
        return new net.minecraft.world.phys.BlockHitResult(
            TypeConverter.toMinecraft(hitResult.pos()),
            toMinecraft(hitResult.direction()),
            TypeConverter.toMinecraft(hitResult.blockPos()),
            hitResult.insideBlock()
        );
    }

    public static BlockHitResult fromMinecraft(net.minecraft.world.phys.BlockHitResult hitResult) {
        return new BlockHitResult(
            TypeConverter.fromMinecraft(hitResult.getLocation()),
            fromMinecraft(hitResult.getDirection()),
            TypeConverter.fromMinecraft(hitResult.getBlockPos()),
            hitResult.isInside()
        );
    }

    public static net.minecraft.core.Direction toMinecraft(Direction direction) {
        return switch (direction) {
            case DOWN -> net.minecraft.core.Direction.DOWN;
            case UP -> net.minecraft.core.Direction.UP;
            case NORTH -> net.minecraft.core.Direction.NORTH;
            case SOUTH -> net.minecraft.core.Direction.SOUTH;
            case WEST -> net.minecraft.core.Direction.WEST;
            case EAST -> net.minecraft.core.Direction.EAST;
        };
    }

    public static Direction fromMinecraft(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }
    public static is.pig.minecraft.api.InteractionHand fromMinecraft(net.minecraft.world.InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> is.pig.minecraft.api.InteractionHand.MAIN_HAND;
            case OFF_HAND -> is.pig.minecraft.api.InteractionHand.OFF_HAND;
        };
    }

    public static net.minecraft.world.InteractionHand toMinecraft(is.pig.minecraft.api.InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> net.minecraft.world.InteractionHand.MAIN_HAND;
            case OFF_HAND -> net.minecraft.world.InteractionHand.OFF_HAND;
        };
    }
}
