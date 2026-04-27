package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.api.IPhysicsEntity;
import is.pig.minecraft.build.api.IWorldInteraction;

/**
 * SPI-based MLG method contract.
 */
public interface MlgMethod {
    String getName();
    boolean isViable(IPhysicsEntity player, IWorldInteraction world);
    ActionRecord execute(IPhysicsEntity player, IWorldInteraction world);
}
