package is.pig.minecraft.build.mlg.prediction;

import is.pig.minecraft.build.api.FallPredictionResult;
import is.pig.minecraft.build.api.IPhysicsEntity;
import is.pig.minecraft.build.api.IPlayerEntity;
import is.pig.minecraft.build.api.IWorldAccess;
import is.pig.minecraft.build.api.IVector3;
import is.pig.minecraft.build.api.Vector3;

import java.util.Optional;

/**
 * Predictive physics engine for simulating player falls.
 * Pure Java, zero Minecraft dependencies.
 */
public class FallSimulator {

    public static Optional<FallPredictionResult> simulate(IPhysicsEntity playerPhysics, IPlayerEntity playerState, IWorldAccess world) {
        if (playerState.onClimbable() || playerState.isInWater() || playerState.isInLava()) {
            return Optional.empty();
        }

        IVector3 currentPos = playerPhysics.getPosition();
        IVector3 currentVel = playerPhysics.getVelocity();
        
        double minX = currentPos.x() - 0.3;
        double maxX = currentPos.x() + 0.3;
        double minY = currentPos.y();
        double maxY = currentPos.y() + 1.8;
        double minZ = currentPos.z() - 0.3;
        double maxZ = currentPos.z() + 0.3;
        
        Box currentBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        float simulatedFallDistance = playerState.fallDistance();

        for (int tick = 1; tick <= 100; tick++) {
            Box expandedBox = currentBox.expandTowards(0, currentVel.y(), 0);
            
            if (world.hasCollisions(playerState, expandedBox.minX, expandedBox.minY, expandedBox.minZ, expandedBox.maxX, expandedBox.maxY, expandedBox.maxZ)) {
                double highestHitY = currentPos.y() + currentVel.y();
                
                IVector3[] corners = {
                        new Vector3(currentBox.minX, currentBox.minY, currentBox.minZ),
                        new Vector3(currentBox.maxX, currentBox.minY, currentBox.minZ),
                        new Vector3(currentBox.minX, currentBox.minY, currentBox.maxZ),
                        new Vector3(currentBox.maxX, currentBox.minY, currentBox.maxZ),
                        new Vector3(currentPos.x(), currentBox.minY, currentPos.z())
                };
                
                for (IVector3 corner : corners) {
                    IVector3 hit = world.clip(corner, corner.add(currentVel.x(), currentVel.y() - 1.0, currentVel.z()));
                    if (hit != null) {
                        highestHitY = Math.max(highestHitY, hit.y());
                    }
                }

                IVector3 hitVec = new Vector3(currentPos.x(), highestHitY, currentPos.z());
                IVector3 impactPos = new Vector3(currentPos.x(), highestHitY - 0.001, currentPos.z());
                IVector3 insidePos = new Vector3(currentPos.x(), highestHitY + 0.001, currentPos.z());
                IVector3 landingPos = insidePos;

                float expectedDamage = world.calculateFallDamage(playerState, simulatedFallDistance, impactPos, insidePos);
                boolean isFatal = expectedDamage >= (playerPhysics.getHealth()) || expectedDamage >= 10.0f;

                return Optional.of(new FallPredictionResult(
                        landingPos,
                        hitVec,
                        tick,
                        simulatedFallDistance,
                        expectedDamage,
                        isFatal
                ));
            }

            simulatedFallDistance -= (float) currentVel.y();
            currentBox = currentBox.move(currentVel);
            currentPos = currentPos.add(currentVel);

            // Gravity (0.08) and Terminal Velocity (applied via drag 0.98)
            currentVel = new Vector3(currentVel.x(), (currentVel.y() - 0.08D) * 0.98D, currentVel.z());
        }

        return Optional.empty();
    }

    private static class Box {
        public final double minX, minY, minZ, maxX, maxY, maxZ;

        public Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public Box expandTowards(double x, double y, double z) {
            double d = minX;
            double e = minY;
            double f = minZ;
            double g = maxX;
            double h = maxY;
            double i = maxZ;
            if (x < 0.0) d += x;
            else if (x > 0.0) g += x;
            if (y < 0.0) e += y;
            else if (y > 0.0) h += y;
            if (z < 0.0) f += z;
            else if (z > 0.0) i += z;
            return new Box(d, e, f, g, h, i);
        }

        public Box move(IVector3 vec) {
            return new Box(minX + vec.x(), minY + vec.y(), minZ + vec.z(), maxX + vec.x(), maxY + vec.y(), maxZ + vec.z());
        }
    }
}
