package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.vehicle.ChestBoat;

public class ChestBoatMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(4)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItemTag(ItemTags.CHEST_BOATS)
                .and(CommonMlgStrategies.requireReplaceableLanding())
                .and(CommonMlgStrategies.requireClearSpace(1, ChestBoat.class)))
            .preparation(CommonMlgStrategies.swapToItemTagAndLookDown(ItemTags.CHEST_BOATS))
            .execution(CommonMlgStrategies.placeAndMountEntity(
                stack -> stack.is(ItemTags.CHEST_BOATS), 
                ChestBoat.class, 
                (client, pos) -> client.level != null && !client.level.getEntitiesOfClass(ChestBoat.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(CommonMlgStrategies.attackEntityWithWeaponSwap(ChestBoat.class))
            .build();
    }
}
