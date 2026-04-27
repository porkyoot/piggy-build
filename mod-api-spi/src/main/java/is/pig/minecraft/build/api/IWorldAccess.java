package is.pig.minecraft.build.api;

/**
 * Pure Java interface for world queries and client-side interactions.
 */
public interface IWorldAccess {
    boolean hasCollisions(IPlayerEntity player, double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    IVector3 clip(IVector3 start, IVector3 end);
    float calculateFallDamage(IPlayerEntity player, float fallDistance, IVector3 impactPos, IVector3 insidePos);
    
    // Config and Input Accessors
    boolean isAutoMlgKeyDown();
    boolean isAutoMlgEnabled();
    boolean isFeatureAutoMlgEnabled();
    void setAutoMlgEnabled(boolean enabled);
    void saveConfig();
    
    // UI Helpers
    void queueIcon(String namespace, String path, int duration, boolean flash);
    
    // State Machine queries
    boolean hasActiveMlgMethod();
    boolean isMlgStateMachineIdle();
    void tickMlgStateMachine();
}
