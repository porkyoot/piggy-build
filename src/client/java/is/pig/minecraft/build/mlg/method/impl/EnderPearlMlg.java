package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.world.UseItemAction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class EnderPearlMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(85)
            .cleanupDifficulty(1)
            .itemConsumptionCost(1)
            .selfDamage(5.0f)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.withinTicks(7))
            .viability(CommonMlgStrategies.requireItem(Items.ENDER_PEARL))
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.ENDER_PEARL))
            .execution((queue, client, prediction) -> {
                InteractionHand hand = InteractionHand.MAIN_HAND;
                if (client.player != null && client.player.getOffhandItem().is(Items.ENDER_PEARL)) {
                    hand = InteractionHand.OFF_HAND;
                }
                queue.enqueue(new UseItemAction(
                        hand,
                        "piggy-build",
                        () -> true
                ) {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            })
            .build();
    }
}
