package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class HayBaleMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(false)
            .fallDamageMultiplier(0.2f)
            .reliabilityScore(85)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.HAY_BLOCK)
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.HAY_BLOCK))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.HAY_BLOCK), 
                (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.HAY_BLOCK)))
            .cleanup(CommonMlgStrategies.breakBlock())
            .build();
    }
}
