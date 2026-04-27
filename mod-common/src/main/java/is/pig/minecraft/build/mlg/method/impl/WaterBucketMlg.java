package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.api.IPhysicsEntity;
import is.pig.minecraft.build.api.IWorldInteraction;
import is.pig.minecraft.build.mlg.method.ActionRecord;
import is.pig.minecraft.build.mlg.method.MlgMethod;

/**
 * Water bucket MLG implementation.
 */
public class WaterBucketMlg implements MlgMethod {

    @Override
    public String getName() {
        return "WaterBucket";
    }

    @Override
    public boolean isViable(IPhysicsEntity player, IWorldInteraction world) {
        return player.getHeldItemIds().contains("minecraft:water_bucket");
    }

    @Override
    public ActionRecord execute(IPhysicsEntity player, IWorldInteraction world) {
        var pos = player.getPosition();
        return new ActionRecord("PLACE", (int)pos.x(), (int)pos.y() - 1, (int)pos.z(), "minecraft:water_bucket");
    }
}
