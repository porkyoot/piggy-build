package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.build.mlg.method.strategy.MlgCleanupStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgPreparationStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.ClickWindowSlotAction;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.HoldKeyAction;
import is.pig.minecraft.lib.action.world.ConsumeItemAction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
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
            .viability(CommonMlgStrategies.requireItem(Items.CHORUS_FRUIT)
                .and(requireTicksToImpactGreaterThan(35)))
            .preparation(swapToItem(Items.CHORUS_FRUIT))
            .execution(holdUseItem(stack -> stack.is(Items.CHORUS_FRUIT), true))
            .cleanup(releaseUseItem())
            .build();
    }

    private static MlgViabilityStrategy requireTicksToImpactGreaterThan(int ticks) {
        return (client, prediction) -> prediction.ticksToImpact() > ticks;
    }

    private static MlgPreparationStrategy swapToItem(Item expectedItem) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            int slot = CommonMlgStrategies.findItemSlot(client, expectedItem);
            if (slot == -1) return;

            if (slot < 9) {
                queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            } else {
                int currentHotbarSlot = client.player.getInventory().selected;
                queue.enqueue(new ClickWindowSlotAction(
                        client.player.inventoryMenu.containerId,
                        slot,
                        currentHotbarSlot,
                        ClickType.SWAP,
                        "piggy-build",
                        ActionPriority.HIGHEST
                ) {
                    @Override
                    protected Optional<Boolean> verify(Minecraft client) {
                        if (client.player != null) {
                            ItemStack stack = client.player.getInventory().getItem(currentHotbarSlot);
                            if (stack.getItem() == expectedItem) {
                                return Optional.of(true);
                            }
                        }
                        return Optional.empty();
                    }
                });
            }
        };
    }

    private static MlgExecutionStrategy holdUseItem(Predicate<ItemStack> itemCondition, boolean state) {
        return (queue, client, prediction) -> {
            if (state) {
                InteractionHand hand = InteractionHand.MAIN_HAND;
                if (itemCondition != null && client.player != null && itemCondition.test(client.player.getOffhandItem())) {
                    hand = InteractionHand.OFF_HAND;
                }
                queue.enqueue(new ConsumeItemAction(hand, "piggy-build", ActionPriority.HIGHEST));
            }
        };
    }

    private static MlgCleanupStrategy releaseUseItem() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                queue.enqueue(new HoldKeyAction(client.options.keyUse, false, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.NORMAL;
                    }
                });
            }

            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                return !PiggyActionQueue.getInstance().hasActions("piggy-build");
            }
        };
    }
}
