package is.pig.minecraft.build.mlg.prediction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;

import java.util.Optional;

public class FallSimulator {

    public static Optional<FallPredictionResult> simulate(LocalPlayer player, ClientLevel world) {
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
                // Mathematically secures the position within the Air block above the geometric floor
                BlockPos landingPos = BlockPos.containing(currentPos.x, highestHitY + 0.001, currentPos.z);

                float expectedDamage = calculateDamage(player, simulatedFallDistance);
                boolean isFatal = expectedDamage >= (player.getHealth() + player.getAbsorptionAmount()) || expectedDamage >= 4.0f;

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

    private static float calculateDamage(LocalPlayer player, float fallDistance) {
        float damage = Math.max(0, fallDistance - 3.0f);
        if (damage <= 0) return 0.0f;

        if (player.level() == null) return damage;

        int totalEPF = 0;
        try {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var featherFalling = registry.getOrThrow(Enchantments.FEATHER_FALLING);
            var protection = registry.getOrThrow(Enchantments.PROTECTION);

            for (ItemStack armor : player.getArmorSlots()) {
                if (!armor.isEmpty()) {
                    totalEPF += EnchantmentHelper.getItemEnchantmentLevel(protection, armor);
                    if (armor == player.getItemBySlot(EquipmentSlot.FEET)) {
                        totalEPF += EnchantmentHelper.getItemEnchantmentLevel(featherFalling, armor) * 3;
                    }
                }
            }
            
            // EPF is capped at 20 (80% reduction max)
            totalEPF = Math.min(20, totalEPF);
            
            if (totalEPF > 0) {
                float reduction = totalEPF * 0.04f; // Each EPF point is 4%
                damage = damage * (1.0f - reduction);
            }
        } catch (Exception e) {
            // Ignore missing registry entries gracefully
        }
        return damage;
    }
}
