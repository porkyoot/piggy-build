package is.pig.minecraft.build.mvc.controller;

import java.util.List;
import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ToolSwapHandler {

    public void onTick(Minecraft client) {
        if (!PiggyConfig.getInstance().isToolSwapEnabled()) {
            return;
        }

        if (client.player == null || client.level == null) {
            return;
        }

        // Don't swap if a screen is open
        if (client.screen != null) {
            return;
        }

        // Only trying to swap if the player is actively attacking/mining
        if (!client.options.keyAttack.isDown()) {
            return;
        }

        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.level.getBlockState(blockHit.getBlockPos());

            int currentSlot = client.player.getInventory().selected;
            ItemStack currentStack = client.player.getMainHandItem();

            // Best candidate initialization
            int bestSlot = currentSlot;
            float bestScore = getToolScore(client, currentStack, state);
            boolean bestDamageable = currentStack.isDamageableItem();

            // Iterate main inventory (0-35)
            // 0-8: Hotbar
            // 9-35: Storage
            for (int i = 0; i < 36; i++) {
                if (i == currentSlot)
                    continue;

                ItemStack stack = client.player.getInventory().getItem(i);
                float score = getToolScore(client, stack, state);
                boolean damageable = stack.isDamageableItem();

                // Logic:
                // 1. Strictly higher score -> Always switch
                // 2. Same score -> Prefer non-damageable (save durability)
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                    bestDamageable = damageable;
                } else if (Math.abs(score - bestScore) < 0.0001f) {
                    if (bestDamageable && !damageable) {
                        bestScore = score;
                        bestSlot = i;
                        bestDamageable = damageable;
                    }
                }
            }

            if (bestSlot != currentSlot) {
                if (bestSlot < 9) {
                    // Hotbar swap - just select it
                    client.player.getInventory().selected = bestSlot;
                    if (client.getConnection() != null) {
                        client.getConnection().send(new ServerboundSetCarriedItemPacket(bestSlot));
                    }
                } else {
                    // Find a valid target slot in the hotbar based on config
                    List<Integer> allowedSlots = PiggyConfig.getInstance().getSwapHotbarSlots();

                    // If no slots allowed, we can't bring it in
                    if (allowedSlots == null || allowedSlots.isEmpty()) {
                        return;
                    }

                    int targetSlot;
                    if (allowedSlots.contains(currentSlot)) {
                        targetSlot = currentSlot;
                    } else {
                        // Fallback to first allowed slot
                        targetSlot = allowedSlots.get(0);
                        // Sanity check to ensure it's a hotbar slot
                        if (targetSlot < 0 || targetSlot > 8)
                            targetSlot = 0;
                    }

                    // Main inventory - Swap into target hotbar slot
                    // Slot Mapping for InventoryMenu (ID 0):
                    // 9-35 (Main Logic Inv) maps exactly to 9-35 (Menu Slots)
                    // We use SWAP click with button = targetSlot
                    client.gameMode.handleInventoryMouseClick(
                            client.player.inventoryMenu.containerId,
                            bestSlot,
                            targetSlot,
                            ClickType.SWAP,
                            client.player);

                    // If we swapped to a different slot than current, we must switch to it
                    if (client.player.getInventory().selected != targetSlot) {
                        client.player.getInventory().selected = targetSlot;
                        if (client.getConnection() != null) {
                            client.getConnection().send(new ServerboundSetCarriedItemPacket(targetSlot));
                        }
                    }
                }
            }
        }
    }

    private float getToolScore(Minecraft client, ItemStack stack, BlockState state) {
        if (stack.isEmpty())
            return 0f;

        float speed = stack.getDestroySpeed(state);

        // Efficiency Bonus
        // Only applies if the tool is effective (speed > 1.0)
        if (speed > 1.0f) {
            int efficiencyLevel = getEnchantmentLevel(client, stack, Enchantments.EFFICIENCY);
            if (efficiencyLevel > 0) {
                speed += (efficiencyLevel * efficiencyLevel + 1);
            }
        }

        // Silk Touch Logic
        boolean hasSilk = getEnchantmentLevel(client, stack, Enchantments.SILK_TOUCH) > 0;
        boolean needsSilk = requiresSilkTouch(state);

        if (needsSilk && hasSilk) {
            speed += 10000.0f; // Massive bonus to ensure selection
        } else if (!needsSilk && hasSilk) {
            speed -= 0.1f; // Slight penalty to preserve Silk Touch durability if not needed
        }

        return speed;
    }

    private int getEnchantmentLevel(Minecraft client, ItemStack stack, ResourceKey<Enchantment> key) {
        if (client.level == null)
            return 0;
        // In 1.21, we need to look up the enchantment in the registry
        try {
            var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var enchant = registry.getOrThrow(key);
            return EnchantmentHelper.getItemEnchantmentLevel(enchant, stack);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean requiresSilkTouch(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.GLASS
                || b == Blocks.GLASS_PANE
                || b == Blocks.ICE
                || b == Blocks.PACKED_ICE
                || b == Blocks.BLUE_ICE
                || b == Blocks.ENDER_CHEST
                || b == Blocks.TURTLE_EGG
                || b == Blocks.BEE_NEST
                || b == Blocks.BEEHIVE
                || b == Blocks.SCULK
                || b == Blocks.SCULK_CATALYST
                || b == Blocks.SCULK_SENSOR
                || b == Blocks.SCULK_SHRIEKER
                || b instanceof StainedGlassBlock
                || b instanceof StainedGlassPaneBlock;
    }
}
