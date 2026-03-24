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
            .reliabilityScore(100)
            .cleanupDifficulty(1)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.WATER_BUCKET)
                .and(CommonMlgStrategies.requireReplaceableLanding())
                .and(CommonMlgStrategies.notUltrawarm()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.WATER_BUCKET))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.WATER_BUCKET), 
                (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.WATER)))
            .cleanup(CommonMlgStrategies.scoopItem(Blocks.WATER, Items.WATER_BUCKET))
            .build();
    }
}
