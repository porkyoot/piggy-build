package is.pig.minecraft.build.mlg.prediction;

import is.pig.minecraft.build.api.IPhysicsEntity;

/**
 * Pure Java fallback damage calculator.
 */
public class HealthPrediction {
    public static float applyDamageResistances(IPhysicsEntity player, float rawDamage) {
        return rawDamage; // Simplified for SPI
    }
}
