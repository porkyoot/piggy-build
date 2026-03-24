package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class SlimeBlockMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(95)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.SLIME_BLOCK)
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.SLIME_BLOCK))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.SLIME_BLOCK), 
                (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.SLIME_BLOCK)))
            .cleanup(CommonMlgStrategies.breakBlock())
            .requiresBounceSettlement(false)
            .build();
    }
}
