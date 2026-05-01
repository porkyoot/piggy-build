package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.build.mlg.method.strategy.MlgCleanupStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgPreparationStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.api.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.HoldKeyAction;
import is.pig.minecraft.lib.action.world.ConsumeItemAction;

import java.util.function.Predicate;

public class ChorusFruitMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(1)
            .itemConsumptionCost(1)
            .isPositionDependent(false)
            .preparationTickOffset(CommonMlgStrategies.fixedPreparationTicks(1000))
            .executionCondition(CommonMlgStrategies.withinTicks(1000))
            .viability(CommonMlgStrategies.requireItem("minecraft:chorus_fruit")
                .and(requireTicksToImpactGreaterThan(35)))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown("minecraft:chorus_fruit"))
            .execution(holdUseItem(true))
            .cleanup(releaseUseItem())
            .build();
    }

    private static MlgViabilityStrategy requireTicksToImpactGreaterThan(int ticks) {
        return (client, prediction) -> prediction.ticksToImpact() > ticks;
    }

    private static MlgExecutionStrategy holdUseItem(boolean state) {
        return (queue, client, prediction) -> {
            if (state) {
                // Simplified hand selection, could be refactored to SPI if needed
                queue.enqueue(new ConsumeItemAction(is.pig.minecraft.api.InteractionHand.MAIN_HAND, "piggy-build", ActionPriority.HIGHEST));
            }
        };
    }

    private static MlgCleanupStrategy releaseUseItem() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Object client, FallPredictionResult prediction) {
                queue.enqueue(new HoldKeyAction("minecraft:use", false, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.NORMAL;
                    }
                });
            }

            @Override
            public boolean isFinished(Object client, FallPredictionResult prediction) {
                return !PiggyActionQueue.getInstance().hasActions("piggy-build");
            }
        };
    }
}
