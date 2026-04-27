package is.pig.minecraft.build.api;

/**
 * Pure Java record for holding the results of a simulated fall.
 */
public record FallPredictionResult(
        IVector3 landingPos,
        IVector3 hitVec,
        int ticksToImpact,
        float fallDistance,
        float expectedDamage,
        boolean isFatal
) {}
