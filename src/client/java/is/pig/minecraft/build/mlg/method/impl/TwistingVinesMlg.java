package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;

public class TwistingVinesMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(85)
            .cleanupDifficulty(3)
            .itemConsumptionCost(1)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem("minecraft:twisting_vines")
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown("minecraft:twisting_vines"))
            .execution(CommonMlgStrategies.interactBlock(stack -> true, 
                (client, pos) -> {
                    WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                    return !worldState.isEmpty(worldState.getCurrentWorldId(), pos);
                }))
            .cleanup(CommonMlgStrategies.breakBlock())
            .build();
    }
}
