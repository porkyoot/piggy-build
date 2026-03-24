package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CobwebMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(90)
            .cleanupDifficulty(5)
            .itemConsumptionCost(1)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.COBWEB)
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.COBWEB))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.COBWEB), 
                (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.COBWEB)))
            .cleanup(CommonMlgStrategies.breakBlockWithToolSwap(Items.COBWEB))
            .build();
    }
}
