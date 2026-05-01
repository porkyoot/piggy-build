package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class TwistingVinesFertilizerMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(10)
            .cleanupDifficulty(2)
            .itemConsumptionCost(1)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem(Items.BONE_MEAL)
                .and((client, prediction) -> client.level != null && client.level.getBlockState(is.pig.minecraft.lib.util.TypeConverter.toMinecraft(prediction.landingPos()).below()).is(Blocks.WARPED_NYLIUM))
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.BONE_MEAL))
            .execution(CommonMlgStrategies.interactBlock(stack -> stack.is(Items.BONE_MEAL), 
                (client, pos) -> client.level != null && (client.level.getBlockState(pos.above()).is(Blocks.TWISTING_VINES) || client.level.getBlockState(pos.above()).is(Blocks.TWISTING_VINES_PLANT))))
            .cleanup(CommonMlgStrategies.breakBlock())
            .build();
    }
}
