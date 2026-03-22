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
            Iterable<VoxelShape> collisions = world.getBlockCollisions(player, currentBox.expandTowards(currentVel));

            if (collisions.iterator().hasNext()) {
                Vec3 hitVec = currentPos.add(currentVel);
                BlockPos landingPos = BlockPos.containing(hitVec);

                float expectedDamage = calculateDamage(player, simulatedFallDistance);
                boolean isFatal = expectedDamage >= (player.getHealth() + player.getAbsorptionAmount());

                return Optional.of(new FallPredictionResult(
                        landingPos,
                        hitVec,
                        tick,
                        simulatedFallDistance,
                        expectedDamage,
                        isFatal
                ));
            }

            simulatedFallDistance += (float) currentVel.y;
            currentBox = currentBox.move(currentVel);
            currentPos = currentPos.add(currentVel);

            currentVel = new Vec3(currentVel.x, (currentVel.y - 0.08D) * 0.98D, currentVel.z);
        }

        return Optional.empty();
    }

    private static float calculateDamage(LocalPlayer player, float fallDistance) {
        float damage = Math.max(0, fallDistance - 3.0f);
        if (damage <= 0) return 0.0f;

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.isEmpty() && player.level() != null) {
            try {
                var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var enchant = registry.getOrThrow(Enchantments.FEATHER_FALLING);
                int featherFallingLevel = EnchantmentHelper.getItemEnchantmentLevel(enchant, boots);
                if (featherFallingLevel > 0) {
                    float reduction = featherFallingLevel * 0.12f; // Each level reduces fall damage by 12%
                    damage = damage * (1.0f - reduction);
                }
            } catch (Exception e) {
                // Ignore missing registry entries gracefully
            }
        }
        return damage;
    }
}
