package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MlgCleanupManager {

    private final java.util.Set<BlockPos> breakQueue = new java.util.HashSet<>();
    private final java.util.Set<Integer> vehiclesToBreak = new java.util.HashSet<>();
    private BlockPos currentlyMining = null;
    private boolean startedDestroying = false;

    public void tick(Minecraft client, LocalPlayer player) {
        processBreakQueue(client, player);
        processVehiclesQueue(client, player);
    }

    public void clearQueues() {
        breakQueue.clear();
        vehiclesToBreak.clear();
    }

    public void queueBlockBreak(BlockPos pos) {
        breakQueue.add(pos);
    }

    public void queueVehicleBreak(int entityId) {
        vehiclesToBreak.add(entityId);
    }

    public void reset() {
        startedDestroying = false;
    }

    private void processBreakQueue(Minecraft client, LocalPlayer player) {
        if (breakQueue.isEmpty()) return;

        java.util.Iterator<BlockPos> it = breakQueue.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            BlockState blockState = client.level.getBlockState(pos);

            if (blockState.isAir() || (!blockState.is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK) && !blockState.is(net.minecraft.world.level.block.Blocks.COBWEB) && !blockState.is(net.minecraft.world.level.block.Blocks.HAY_BLOCK) && !(blockState.getBlock() instanceof net.minecraft.world.level.block.BedBlock))) {
                it.remove();
                if (pos.equals(currentlyMining)) {
                    currentlyMining = null;
                    startedDestroying = false;
                }
                continue;
            }

            if (player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) <= 4.5) {
                if (!pos.equals(currentlyMining) || !startedDestroying) {
                    swapToBestToolFor(client, pos, blockState, null);
                    client.gameMode.startDestroyBlock(pos, net.minecraft.core.Direction.UP);
                    currentlyMining = pos;
                    startedDestroying = true;
                } else {
                    client.gameMode.continueDestroyBlock(pos, net.minecraft.core.Direction.UP);
                }
                return;
            } else if (pos.equals(currentlyMining)) {
                startedDestroying = false;
            }
        }
    }

    private void processVehiclesQueue(Minecraft client, LocalPlayer player) {
        if (vehiclesToBreak.isEmpty()) return;

        java.util.Iterator<Integer> it = vehiclesToBreak.iterator();
        while (it.hasNext()) {
            int entityId = it.next();
            net.minecraft.world.entity.Entity entity = client.level.getEntity(entityId);

            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }

            if (entity.hasPassenger(player) || player.getVehicle() == entity) {
                continue;
            }

            if (player.distanceTo(entity) <= 4.5) {
                swapToBestToolFor(client, null, null, entity);
                if (player.getAttackStrengthScale(0.0f) >= 1.0f) {
                    client.gameMode.attack(player, entity);
                    player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    player.resetAttackStrengthTicker();
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Striking generated vehicle organically...");
                }
            }
        }
    }

    private void swapToBestToolFor(Minecraft client, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        int bestSlot = -1;
        try {
            Class<?> inputCtrlClass = Class.forName("is.pig.minecraft.inventory.mvc.controller.InputController");
            if (entity != null) {
                Object weaponHandler = inputCtrlClass.getMethod("getWeaponSwapHandler").invoke(null);
                bestSlot = (int) weaponHandler.getClass().getMethod("getBestWeaponSlot", Minecraft.class, net.minecraft.world.entity.Entity.class).invoke(weaponHandler, client, entity);
            } else if (state != null && pos != null) {
                Object toolHandler = inputCtrlClass.getMethod("getToolSwapHandler").invoke(null);
                bestSlot = (int) toolHandler.getClass().getMethod("getBestToolSlot", Minecraft.class, BlockPos.class, BlockState.class).invoke(toolHandler, client, pos, state);
            }
        } catch (Exception e) {
            // fallback handled natively below
        }

        if (bestSlot == -1 && state != null && state.is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            for (int i = 0; i < 9; i++) {
                net.minecraft.world.item.Item invItem = client.player.getInventory().getItem(i).getItem();
                if (invItem instanceof net.minecraft.world.item.SwordItem || invItem == net.minecraft.world.item.Items.SHEARS) {
                    bestSlot = i;
                    break;
                }
            }
        }

        if (bestSlot != -1 && bestSlot != client.player.getInventory().selected) {
            client.player.getInventory().selected = bestSlot;
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundSetCarriedItemPacket(bestSlot));
                PiggyBuildClient.LOGGER.info("[AutoMLG] Requested piggy-inventory tool-swap targeting hotbar slot {}.", bestSlot);
            }
        }
    }
}
