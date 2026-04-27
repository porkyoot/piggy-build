package is.pig.minecraft.build.mlg.method.impl;

import is.pig.minecraft.build.api.IPhysicsEntity;
import is.pig.minecraft.build.api.IWorldInteraction;
import is.pig.minecraft.build.mlg.method.ActionRecord;
import is.pig.minecraft.build.mlg.method.MlgMethod;

/**
 * Bed MLG implementation.
 */
public class BedMlg implements MlgMethod {

    @Override
    public String getName() {
        return "Bed";
    }

    @Override
    public boolean isViable(IPhysicsEntity player, IWorldInteraction world) {
        for (String id : player.getHeldItemIds()) {
            if (id.endsWith("_bed")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ActionRecord execute(IPhysicsEntity player, IWorldInteraction world) {
        var pos = player.getPosition();
        String bedId = "minecraft:red_bed";
        for (String id : player.getHeldItemIds()) {
            if (id.endsWith("_bed")) {
                bedId = id;
                break;
            }
        }
        return new ActionRecord("PLACE", (int)pos.x(), (int)pos.y() - 1, (int)pos.z(), bedId);
    }
}
