package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class PowderSnowBucketMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(98)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.POWDER_SNOW_BUCKET)
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.POWDER_SNOW_BUCKET))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.POWDER_SNOW_BUCKET), 
                (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.POWDER_SNOW)))
            .cleanup(CommonMlgStrategies.scoopItem(Blocks.POWDER_SNOW, Items.POWDER_SNOW_BUCKET))
            .build();
    }
}
