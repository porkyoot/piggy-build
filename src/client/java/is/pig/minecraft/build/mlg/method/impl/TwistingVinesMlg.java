package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class TwistingVinesMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(85)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.TWISTING_VINES)
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.TWISTING_VINES))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.TWISTING_VINES), 
                (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.TWISTING_VINES)))
            .cleanup(CommonMlgStrategies.breakBlock())
            .build();
    }
}
