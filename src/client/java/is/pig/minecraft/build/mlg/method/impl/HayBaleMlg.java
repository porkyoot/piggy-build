package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;

public class HayBaleMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(false)
            .fallDamageMultiplier(0.2f)
            .reliabilityScore(85)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem("minecraft:hay_block")
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown("minecraft:hay_block"))
            .execution(CommonMlgStrategies.interactBlock(stack -> true, 
                (client, pos) -> {
                    WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                    return !worldState.isEmpty(worldState.getCurrentWorldId(), pos);
                }))
            .cleanup(CommonMlgStrategies.breakBlock())
            .build();
    }
}
