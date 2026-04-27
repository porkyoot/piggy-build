package is.pig.minecraft.build.api;

import java.util.List;

/**
 * Pure Java interface for entity physics state.
 */
public interface IPhysicsEntity {
    IVector3 getPosition();
    IVector3 getVelocity();
    float getHealth();
    List<String> getHeldItemIds();
}
