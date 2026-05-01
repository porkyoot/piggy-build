package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;

public class WaterBucketMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .dynamicReliabilityScore((client, prediction) -> {
                WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                String worldId = worldState.getCurrentWorldId();
                BlockPos belowPos = new BlockPos(prediction.landingPos().x(), prediction.landingPos().y() - 1, prediction.landingPos().z());
                
                // Simplified reliability score logic using SPI
                if (!worldState.isEmpty(worldId, belowPos)) {
                    return 100;
                }
                return 100;
            })
            .cleanupDifficulty(1)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItem("minecraft:water_bucket")
                .and(CommonMlgStrategies.requireWaterloggableOrReplaceableLanding())
                .and(CommonMlgStrategies.notUltrawarm())
                .and(CommonMlgStrategies.notUnsafeWaterloggable()))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown("minecraft:water_bucket"))
            .execution((queue, client, prediction) -> {
                CommonMlgStrategies.interactBlock(stack -> true, // Simplified condition
                    (c, pos) -> {
                        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                        String worldId = worldState.getCurrentWorldId();
                        // Check if water is placed
                        return !worldState.isEmpty(worldId, pos);
                    })
                .queueExecution(queue, client, prediction);
            })
            .cleanup(CommonMlgStrategies.scoopItem("minecraft:water", "minecraft:water_bucket"))
            .build();
    }
}
