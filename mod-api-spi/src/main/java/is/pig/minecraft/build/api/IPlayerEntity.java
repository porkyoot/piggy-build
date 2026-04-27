package is.pig.minecraft.build.api;

/**
 * Pure Java interface for player state tracking.
 */
public interface IPlayerEntity {
    boolean onClimbable();
    boolean isInWater();
    boolean isInLava();
    IVector3 position();
    IVector3 getDeltaMovement();
    float fallDistance();
    
    // Bounding Box components
    double minX();
    double minY();
    double minZ();
    double maxX();
    double maxY();
    double maxZ();
    
    float getHealth();
    float getAbsorptionAmount();
    boolean onGround();
    boolean isFallFlying();
    boolean isDeadOrDying();
}
