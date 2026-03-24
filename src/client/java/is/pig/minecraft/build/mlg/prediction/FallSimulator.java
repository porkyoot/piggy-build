package is.pig.minecraft.build.mlg.prediction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class FallSimulator {

    public static Optional<FallPredictionResult> simulate(LocalPlayer player, ClientLevel world) {
        if (player.onClimbable() || player.isInWater() || player.isInLava()) {
            return Optional.empty();
        }

        Vec3 currentPos = player.position();
        Vec3 currentVel = player.getDeltaMovement();
        AABB currentBox = player.getBoundingBox();
        float simulatedFallDistance = player.fallDistance;

        for (int tick = 1; tick <= 100; tick++) {
            Iterable<VoxelShape> collisions = world.getBlockCollisions(player, currentBox.expandTowards(0, currentVel.y, 0));

            if (collisions.iterator().hasNext()) {
                double highestHitY = currentPos.y + currentVel.y;
                
                Vec3[] corners = {
                        new Vec3(currentBox.minX, currentBox.minY, currentBox.minZ),
                        new Vec3(currentBox.maxX, currentBox.minY, currentBox.minZ),
                        new Vec3(currentBox.minX, currentBox.minY, currentBox.maxZ),
                        new Vec3(currentBox.maxX, currentBox.minY, currentBox.maxZ),
                        new Vec3(currentPos.x, currentBox.minY, currentPos.z)
                };
                
                for (Vec3 corner : corners) {
                    net.minecraft.world.phys.HitResult hit = world.clip(new net.minecraft.world.level.ClipContext(
                            corner,
                            corner.add(currentVel.x, currentVel.y - 1.0, currentVel.z),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            player
                    ));
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        highestHitY = Math.max(highestHitY, hit.getLocation().y);
                    }
                }

                Vec3 hitVec = new Vec3(currentPos.x, highestHitY, currentPos.z);
                
                BlockPos impactPos = BlockPos.containing(currentPos.x, highestHitY - 0.001, currentPos.z);
                BlockPos insidePos = BlockPos.containing(currentPos.x, highestHitY + 0.001, currentPos.z);
                
                // Mathematically secures the position within the Air block above the geometric floor
                BlockPos landingPos = insidePos;

                float expectedDamage = HealthPrediction.calculateFallDamage(player, simulatedFallDistance, impactPos, insidePos, world);
                boolean isFatal = expectedDamage >= (player.getHealth() + player.getAbsorptionAmount()) || expectedDamage >= 10.0f;

                return Optional.of(new FallPredictionResult(
                        landingPos,
                        hitVec,
                        tick,
                        simulatedFallDistance,
                        expectedDamage,
                        isFatal
                ));
            }

            simulatedFallDistance -= (float) currentVel.y;
            currentBox = currentBox.move(currentVel);
            currentPos = currentPos.add(currentVel);

            currentVel = new Vec3(currentVel.x, (currentVel.y - 0.08D) * 0.98D, currentVel.z);
        }

        return Optional.empty();
    }
}
