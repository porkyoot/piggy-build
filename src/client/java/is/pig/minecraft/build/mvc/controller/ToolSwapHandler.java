package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
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

        // Only trying to swap if the player is actively attacking/mining
        if (!client.options.keyAttack.isDown()) {
            return;
        }

        if (client.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
            BlockState state = client.level.getBlockState(blockHit.getBlockPos());

            // Current tool
            ItemStack currentStack = client.player.getMainHandItem();
            float currentSpeed = currentStack.getDestroySpeed(state);

            int bestSlot = -1;
            float bestSpeed = currentSpeed;

            // Check hotbar (0-8)
            for (int i = 0; i < 9; i++) {
                if (i == client.player.getInventory().selected)
                    continue;

                ItemStack stack = client.player.getInventory().getItem(i);
                float speed = stack.getDestroySpeed(state);

                // Simple logic: strictly faster
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1) {
                client.player.getInventory().selected = bestSlot;
            }
        }
    }
}
