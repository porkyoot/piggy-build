package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.player.HoldKeyAction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Items;

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
            .viability(CommonMlgStrategies.requireItemClass(BedItem.class)
                .and(CommonMlgStrategies.requireReplaceableLanding()))
            .preparation(CommonMlgStrategies.swapToItemClassAndLookDown(BedItem.class))
            .execution((queue, client, prediction) -> {
                CommonMlgStrategies.interactBlock(
                    stack -> stack.getItem() instanceof BedItem, 
                    (c, pos) -> c.level != null && c.level.getBlockState(pos.above()).is(BlockTags.BEDS)
                ).queueExecution(queue, client, prediction);
                
                queue.enqueue(new HoldKeyAction(client.options.keyUse, false, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            })
            .cleanup(CommonMlgStrategies.breakBlockWithToolSwap(Items.WOODEN_AXE))
            .build();
    }
}
