package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import is.pig.minecraft.lib.util.TypeConverter;

import java.util.List;

public class ExistingMountableMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(99)
            .cleanupDifficulty(0)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(requireExistingMountableEntity())
            .preparation(CommonMlgStrategies.lookDownWithoutItemSwap())
            .execution(mountExistingEntity())
            .cleanup(CommonMlgStrategies.noCleanup())
            .build();
    }

    private static boolean isExistingMountable(Minecraft client, Entity e) {
        if (client.player != null && e.hasPassenger(client.player)) return false;
        if (!e.getPassengers().isEmpty()) return false;
        if (e instanceof net.minecraft.world.entity.vehicle.Boat) return true;
        if (e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) return true;
        if (e instanceof net.minecraft.world.entity.Saddleable s && s.isSaddled()) return true;
        if (e instanceof net.minecraft.world.entity.animal.horse.AbstractHorse h && h.isTamed()) return true;
        return false;
    }

    private static MlgViabilityStrategy requireExistingMountableEntity() {
        return (client, prediction) -> {
            if (client.level == null) return false;
            List<? extends Entity> entities = client.level.getEntitiesOfClass(
                    Entity.class, 
                    new AABB(TypeConverter.toMinecraft(prediction.landingPos())).inflate(3), 
                    e -> isExistingMountable(client, e)
            );
            return !entities.isEmpty();
        };
    }

    private static MlgExecutionStrategy mountExistingEntity() {
        return (queue, client, prediction) -> {
            queue.enqueue(CommonMlgStrategies.createSpamMountAction(prediction, null, e -> isExistingMountable(client, e)));
        };
    }
}
