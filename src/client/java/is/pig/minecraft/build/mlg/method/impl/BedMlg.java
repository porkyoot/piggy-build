package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.ItemDataAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.api.ActionPriority;
import is.pig.minecraft.lib.action.player.HoldKeyAction;

public class BedMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(false)
            .fallDamageMultiplier(0.5f)
            .reliabilityScore(70)
            .cleanupDifficulty(5)
            .requiresBounceSettlement(false)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItemTag("minecraft:beds")
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemTagAndLookDown("minecraft:beds"))
            .execution((queue, client, prediction) -> {
                ItemDataAdapter itemData = PiggyServiceRegistry.getItemDataAdapter();
                CommonMlgStrategies.interactBlock(
                    stack -> itemData.hasTag(stack, "minecraft:beds"), 
                    (c, pos) -> {
                        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                        return !worldState.isEmpty(worldState.getCurrentWorldId(), pos);
                    }
                ).queueExecution(queue, client, prediction);
                
                queue.enqueue(new HoldKeyAction("minecraft:use", false, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            })
            .cleanup(CommonMlgStrategies.breakBlock())
            .build();
    }
}
