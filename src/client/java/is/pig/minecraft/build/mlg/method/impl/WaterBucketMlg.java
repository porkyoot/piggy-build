package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class WaterBucketMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .dynamicReliabilityScore((client, prediction) -> {
                if (client.level == null) return 100;
                net.minecraft.world.level.block.state.BlockState state = client.level.getBlockState(prediction.landingPos().below());
                if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                    net.minecraft.world.phys.shapes.VoxelShape shape = state.getCollisionShape(client.level, prediction.landingPos().below());
                    if (!shape.isEmpty() && shape.max(net.minecraft.core.Direction.Axis.Y) > 1.0) {
                        return 30; // Unsafe waterloggable (e.g., Fences/Walls) -> de-prioritize heavily
                    }
                }
                return 100;
            })
            .cleanupDifficulty(1)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.WATER_BUCKET)
                .and(CommonMlgStrategies.requireReplaceableLanding())
                .and(CommonMlgStrategies.notUltrawarm()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.WATER_BUCKET))
            .execution((queue, client, prediction) -> {
                if (client.level == null) return;
                net.minecraft.world.level.block.state.BlockState state = client.level.getBlockState(prediction.landingPos().below());
                boolean isWaterloggable = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
                boolean isUnsafe = false;
                if (isWaterloggable) {
                    net.minecraft.world.phys.shapes.VoxelShape shape = state.getCollisionShape(client.level, prediction.landingPos().below());
                    isUnsafe = !shape.isEmpty() && shape.max(net.minecraft.core.Direction.Axis.Y) > 1.0;
                }
                
                if (isWaterloggable && isUnsafe) {
                    // Flexible bloc placement ABOVE the unsafe water loggable block
                    is.pig.minecraft.lib.placement.BlockPlacer.placeBlock(
                            is.pig.minecraft.lib.placement.BlockPlacer.createHitResult(prediction.landingPos().above(), net.minecraft.core.Direction.DOWN),
                            net.minecraft.world.InteractionHand.MAIN_HAND,
                            true
                    );
                } else {
                    CommonMlgStrategies.interactBlock(stack -> stack.is(Items.WATER_BUCKET), 
                        (c, pos) -> c.level != null && c.level.getBlockState(pos.above()).is(Blocks.WATER))
                    .queueExecution(queue, client, prediction);
                }
            })
            .cleanup(CommonMlgStrategies.scoopItem(Blocks.WATER, Items.WATER_BUCKET))
            .build();
    }
}
