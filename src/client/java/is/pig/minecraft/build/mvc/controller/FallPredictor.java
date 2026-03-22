package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class FallPredictor {

    public static BlockPos predictImpact(Minecraft client, LocalPlayer player) {
        BlockPos impactPos = null;
        net.minecraft.world.phys.AABB baseBox = player.getBoundingBox();

        Vec3 simPos = player.position();
        Vec3 simVel = player.getDeltaMovement();

        double pingMs = 0;
        if (client.getConnection() != null && client.getConnection().getPlayerInfo(player.getUUID()) != null) {
            pingMs = client.getConnection().getPlayerInfo(player.getUUID()).getLatency();
        }
        double pingTicks = Math.min(10.0, pingMs / 50.0);
        double pingOffset = Math.abs(simVel.y * pingTicks);
        double placementThreshold = 5.0 + pingOffset;

        // Dynamically simulate exactly as many ticks as it mathematically takes to trace the threshold deep, 
        // to structurally guarantee we identify the placement target BEFORE dropping within bounding-box collision distances!
        boolean isPearlHotbar = false;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).getItem() == net.minecraft.world.item.Items.ENDER_PEARL) {
                isPearlHotbar = true;
                break;
            }
        }
        
        double minXOffset = baseBox.minX - player.getX();
        double maxXOffset = baseBox.maxX - player.getX();
        double minZOffset = baseBox.minZ - player.getZ();
        double maxZOffset = baseBox.maxZ - player.getZ();
        double yOffset = baseBox.minY - player.getY();

        // Vastly reduce artificial horizontal decay drag if the user is maintaining native WASD vectoring in air!
        double hFric = (player.xxa != 0.0f || player.zza != 0.0f) ? 0.99 : 0.91;
        
        for (int ticks = 1; ticks <= 80; ticks++) { 
            Vec3 nextPos = simPos.add(simVel);
            
            Vec3[] currentCorners = new Vec3[] {
                new Vec3(simPos.x + minXOffset, simPos.y + yOffset, simPos.z + minZOffset),
                new Vec3(simPos.x + maxXOffset, simPos.y + yOffset, simPos.z + minZOffset),
                new Vec3(simPos.x + minXOffset, simPos.y + yOffset, simPos.z + maxZOffset),
                new Vec3(simPos.x + maxXOffset, simPos.y + yOffset, simPos.z + maxZOffset)
            };
            
            Vec3[] nextCorners = new Vec3[] {
                new Vec3(nextPos.x + minXOffset, nextPos.y + yOffset, nextPos.z + minZOffset),
                new Vec3(nextPos.x + maxXOffset, nextPos.y + yOffset, nextPos.z + minZOffset),
                new Vec3(nextPos.x + minXOffset, nextPos.y + yOffset, nextPos.z + maxZOffset),
                new Vec3(nextPos.x + maxXOffset, nextPos.y + yOffset, nextPos.z + maxZOffset)
            };
            
            for (int c = 0; c < 4; c++) {
                net.minecraft.world.phys.BlockHitResult hit = client.level.clip(new net.minecraft.world.level.ClipContext(
                    currentCorners[c], nextCorners[c], 
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, 
                    net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY, 
                    player
                ));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    BlockPos hitPos = hit.getBlockPos();
                    if (impactPos == null || hitPos.getY() > impactPos.getY()) {
                        impactPos = hitPos;
                    }
                }
            }

            if (impactPos != null) {
                break;
            }

            simPos = nextPos;
            simVel = new Vec3(simVel.x * hFric, (simVel.y - 0.08) * 0.98, simVel.z * hFric);

            // Stop aggressively sweeping horizontally if we've successfully mapped past our absolute native 4.5 execution limit entirely 
            // (prevents artificial curve predictions casting into distant walls on the way down)
            if (player.position().y - simPos.y >= placementThreshold && !isPearlHotbar) {
                break;
            }
        }

        return impactPos;
    }
}
