package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.lib.inventory.search.InventorySearcher;
import is.pig.minecraft.lib.inventory.search.ItemCondition;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.phys.AABB;

public class MinecartMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(98)
            .cleanupDifficulty(3)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(requireRailAndMinecart())
            .preparation(CommonMlgStrategies.swapToItemClassAndLookDown(MinecartItem.class))
            .execution(placeMinecartAndMountEntity())
            .cleanup(CommonMlgStrategies.attackEntityWithWeaponSwap(AbstractMinecart.class))
            .build();
    }

    private static MlgViabilityStrategy requireRailAndMinecart() {
        return (client, prediction) -> {
            if (client.level == null || client.player == null) return false;
            if (!client.level.getBlockState(prediction.landingPos()).is(BlockTags.RAILS) &&
                !client.level.getBlockState(prediction.landingPos().above()).is(BlockTags.RAILS)) {
                return false;
            }
            ItemCondition condition = stack -> stack.getItem() instanceof MinecartItem;
            if (condition.matches(client.player.getOffhandItem())) return true;
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    private static MlgExecutionStrategy placeMinecartAndMountEntity() {
        return (queue, client, prediction) -> {
            CommonMlgStrategies.interactSpecificBlock(
                stack -> stack.getItem() instanceof MinecartItem, 
                p -> {
                    if (client.level != null && client.level.getBlockState(p.landingPos()).is(BlockTags.RAILS)) {
                        return p.landingPos();
                    }
                    return p.landingPos().above();
                }, 
                (c, pos) -> c.level != null && !c.level.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(2), e -> true).isEmpty()
            ).queueExecution(queue, client, prediction);
            
            queue.enqueue(CommonMlgStrategies.createSpamMountAction(
                prediction, 
                stack -> stack.getItem() instanceof MinecartItem, 
                e -> e instanceof AbstractMinecart
            ));
        };
    }
}
