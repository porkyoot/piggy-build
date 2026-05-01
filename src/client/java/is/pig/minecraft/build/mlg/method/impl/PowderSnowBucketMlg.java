package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;

public class PowderSnowBucketMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(98)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem("minecraft:powder_snow_bucket")
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown("minecraft:powder_snow_bucket"))
            .execution(CommonMlgStrategies.interactBlock(stack -> true, 
                (client, pos) -> {
                    WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                    return !worldState.isEmpty(worldState.getCurrentWorldId(), pos);
                }))
            .cleanup(CommonMlgStrategies.scoopItem("minecraft:powder_snow", "minecraft:powder_snow_bucket"))
            .build();
    }
}
