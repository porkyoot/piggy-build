package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.vehicle.Boat;

public class BoatMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(4)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(CommonMlgStrategies.requireItemTag(ItemTags.BOATS)
                .and(CommonMlgStrategies.requireReplaceableLanding())
                .and(CommonMlgStrategies.requireClearSpace(1, Boat.class)))
            .preparation(CommonMlgStrategies.swapToItemTagAndLookDown(ItemTags.BOATS))
            .execution(CommonMlgStrategies.placeAndMountEntity(
                stack -> stack.is(ItemTags.BOATS), 
                Boat.class, 
                (client, pos) -> client.level != null && !client.level.getEntitiesOfClass(Boat.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(CommonMlgStrategies.attackEntityWithWeaponSwap(Boat.class))
            .build();
    }
}
