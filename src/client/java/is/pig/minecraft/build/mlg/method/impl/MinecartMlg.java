package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.inventory.util.InventorySearcher;
import is.pig.minecraft.inventory.util.ItemCondition;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.phys.AABB;
import is.pig.minecraft.lib.util.TypeConverter;

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
            if (!client.level.getBlockState(TypeConverter.toMinecraft(prediction.landingPos())).is(BlockTags.RAILS) &&
                !client.level.getBlockState(TypeConverter.toMinecraft(prediction.landingPos()).above()).is(BlockTags.RAILS)) {
                return false;
            }
            ItemCondition condition = stack -> PiggyServiceRegistry.getItemDataAdapter().getItemId(stack).contains("minecart");
            if (condition.matches(client.player.getOffhandItem())) return true;
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    private static MlgExecutionStrategy placeMinecartAndMountEntity() {
        return (queue, client, prediction) -> {
            CommonMlgStrategies.interactSpecificBlock(
                stack -> PiggyServiceRegistry.getItemDataAdapter().getItemId(stack).contains("minecart"), 
                p -> {
                    if (client.level != null && client.level.getBlockState(TypeConverter.toMinecraft(p.landingPos())).is(BlockTags.RAILS)) {
                        return TypeConverter.toMinecraft(p.landingPos());
                    }
                    return TypeConverter.toMinecraft(p.landingPos()).above();
                }, 
                (c, pos) -> c.level != null && !c.level.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos).inflate(2), e -> true).isEmpty()
            ).queueExecution(queue, client, prediction);
            
            queue.enqueue(CommonMlgStrategies.createSpamMountAction(
                prediction, 
                stack -> PiggyServiceRegistry.getItemDataAdapter().getItemId(stack).contains("minecart"), 
                e -> e instanceof AbstractMinecart
            ));
        };
    }
}
