package is.pig.minecraft.build.mlg.prediction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

public class HealthPrediction {

    public static float calculateFallDamage(LocalPlayer player, float fallDistance, BlockPos impactPos, BlockPos insidePos, ClientLevel world) {
        net.minecraft.world.level.block.state.BlockState impactState = world.getBlockState(impactPos);
        net.minecraft.world.level.block.state.BlockState insideState = world.getBlockState(insidePos);

        // Carpets transfer impact physics to the block below them natively
        if (impactState.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock) {
            impactState = world.getBlockState(impactPos.below());
        }
        
        // Complete fall damage negations
        if (insideState.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || impactState.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            return 0.0f; // Water or waterlogged blocks completely negate
        }
        if (insideState.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) || impactState.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
            return 0.0f;
        }
        if (insideState.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW) || impactState.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) {
            return 0.0f; // Powder snow arrests falls entirely
        }
        if (insideState.is(net.minecraft.world.level.block.Blocks.COBWEB) || impactState.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            return 0.0f;
        }
        if (impactState.is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK) && !player.isSuppressingBounce()) {
            return 0.0f;
        }
        if (insideState.is(net.minecraft.tags.BlockTags.CLIMBABLE) || impactState.is(net.minecraft.tags.BlockTags.CLIMBABLE)) {
            return 0.0f; // Vines, scaffolding, ladders
        }
        if (insideState.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) || impactState.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)) {
            return 0.0f;
        }

        // Damage multipliers and reductions
        float damageMultiplier = 1.0f;
        if (impactState.is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)) {
            damageMultiplier = 0.2f;
        } else if (impactState.is(net.minecraft.world.level.block.Blocks.HONEY_BLOCK)) {
            damageMultiplier = 0.2f;
        } else if (impactState.is(net.minecraft.tags.BlockTags.BEDS)) {
            damageMultiplier = 0.5f;
        }

        float damage = Math.max(0, fallDistance - 3.0f);
        if (damage <= 0) return 0.0f;
        
        damage *= damageMultiplier;

        return applyDamageResistances(player, damage);
    }

    public static float applyDamageResistances(LocalPlayer player, float rawDamage) {
        if (rawDamage <= 0) return 0.0f;
        float damage = rawDamage;

        // Potion of Resistance reduces 20% per tier (Amplifier + 1)
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE)) {
            int amplifier = player.getEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE).getAmplifier();
            damage -= damage * ((amplifier + 1) * 0.2f);
        }
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
