package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.PiggyBuildClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class AutoMlgHandler {

    private boolean wasKeyDown = false;

    private MlgPhase currentPhase = MlgPhase.IDLE;
    private MlgContext currentContext = null;
    
    private int cleanupTicks = 0;
    private int cleanupDelayTicks = 0;
    private final MlgCleanupManager cleanupManager = new MlgCleanupManager();
    private final MlgActionQueue actionQueue = new MlgActionQueue();
    private net.minecraft.resources.ResourceKey<Level> lastDimension = null;

    private boolean queuedPearlDeploy = false;

    public void onTick(Minecraft client) {
        if (InputController.autoMlgKey == null) return;
        
        LocalPlayer player = client.player;
        if (player != null && queuedPearlDeploy) {
            queuedPearlDeploy = false;
            client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            PiggyBuildClient.LOGGER.info("[AutoMLG] Executed queued Pearl Deploy natively with preserved velocity!");
            currentPhase = MlgPhase.CLEANUP;
            cleanupDelayTicks = 0;
            return;
        }

        boolean isKeyDown = InputController.autoMlgKey.isDown();
        if (isKeyDown && !wasKeyDown) {
            PiggyBuildConfig config = PiggyBuildConfig.getInstance();
            boolean newState = !config.isAutoMlgEnabled();
            config.setAutoMlgEnabled(newState);
            ConfigPersistence.save();
            PiggyBuildClient.LOGGER.info("[AutoMLG] Toggled to: {}", newState);
            
            if (newState) {
                is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/auto_mlg.png"),
                    2000, false
                );
            }
        }
        wasKeyDown = isKeyDown;

        if (!PiggyBuildConfig.getInstance().isFeatureAutoMlgEnabled() ||
            !PiggyBuildConfig.getInstance().isAutoMlgEnabled()) {
            resetMlg();
            return;
        }

        if (player == null || client.level == null || client.gameMode == null) {
            resetMlg();
            return;
        }

        if (lastDimension == null || lastDimension != client.level.dimension()) {
            lastDimension = client.level.dimension();
            resetMlg();
            return;
        }

        if (player.hurtTime > 0) {
            PiggyBuildClient.LOGGER.info("[AutoMLG] Player took damage. Aborting sequence.");
            resetMlg();
            return;
        }

        cleanupManager.tick(client, player);

        if (player.isDeadOrDying()) {
            cleanupManager.clearQueues();
            actionQueue.clearQueue();
            resetMlg();
            return;
        }

        if (currentPhase == MlgPhase.TARGETING || currentPhase == MlgPhase.DEPLOYING) {
            if (currentContext != null && currentContext.targetPos != null) {
                double vX = player.getDeltaMovement().x;
                double vZ = player.getDeltaMovement().z;
                double lateralVel = Math.sqrt(vX * vX + vZ * vZ);
                
                double dX = player.getX() - (currentContext.targetPos.getX() + 0.5);
                double dZ = player.getZ() - (currentContext.targetPos.getZ() + 0.5);
                double driftDist = Math.sqrt(dX * dX + dZ * dZ);
                
                if (driftDist > 1.5 || lateralVel > 0.3) {
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Massive lateral drift detected (Dist: {}, Vel: {}). Forcing recalculation...", driftDist, lateralVel);
                    actionQueue.clearQueue();
                    currentPhase = MlgPhase.TARGETING;
                }
            }
        }

        if (!actionQueue.isEmpty()) {
            if (actionQueue.processNext(client, player)) {
                return;
            }
        }

        switch (currentPhase) {
            case IDLE:
                handleIdle(client, player);
                break;
            case TARGETING:
                handleTargeting(client, player);
                break;
            case DEPLOYING:
                handleDeploying(client, player);
                break;
            case AWAITING_COLLISION:
                handleAwaitingCollision(client, player);
                break;
            case BOUNCING_SUSPEND:
                handleBouncingSuspend(client, player);
                break;
            case CLEANUP:
                handleCleanup(client, player);
                break;
        }
    }

    private void handleIdle(Minecraft client, LocalPlayer player) {
        if (player.fallDistance > 3.0f && player.getDeltaMovement().y < -0.1 && !player.isSpectator() && !player.isCreative() && !player.isFallFlying()) {
            currentContext = new MlgContext();
            currentPhase = MlgPhase.TARGETING;
            is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/auto_mlg.png"),
                1000, true
            );
            PiggyBuildClient.LOGGER.info("[AutoMLG] Fall detected (fallDistance: {}). Transitioning to TARGETING phase.", player.fallDistance);
        }
    }

    private void handleTargeting(Minecraft client, LocalPlayer player) {
        if (currentContext == null) return;
        
        BlockPos interceptPos = FallPredictor.predictImpact(client, player);
        if (interceptPos == null) {
            return;
        }

        currentContext.impactPos = interceptPos;
        currentContext.targetPos = interceptPos.above();

        int bestSlot = MlgItemEvaluator.findMlgItem(player, client, player.fallDistance, client.level.getBlockState(currentContext.impactPos), currentContext.targetPos, currentContext.failedSlots);
        
        if (bestSlot == -1) {
            PiggyBuildClient.LOGGER.info("[AutoMLG] No valid MLG items found during TARGETING phase.");
            resetMlg();
            return;
        }

        if (bestSlot >= 9) {
            client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, bestSlot, 8, net.minecraft.world.inventory.ClickType.SWAP, player);
            PiggyBuildClient.LOGGER.info("[AutoMLG] Swapped MLG item from inventory slot {} to hotbar slot 8", bestSlot);
            currentContext.mlgItemSlot = 8;
            currentContext.originalSlot = bestSlot;
        } else {
            currentContext.mlgItemSlot = bestSlot;
            currentContext.originalSlot = bestSlot;
        }

        Item bestItem = player.getInventory().getItem(currentContext.mlgItemSlot).getItem();
        if (bestItem == Items.WATER_BUCKET || bestItem == Items.LAVA_BUCKET || bestItem == Items.POWDER_SNOW_BUCKET) {
            currentContext.mlgType = MlgContext.MlgType.LIQUID;
        } else if (bestItem == Items.ENDER_PEARL || bestItem == Items.CHORUS_FRUIT) {
            currentContext.mlgType = MlgContext.MlgType.PEARL;
        } else if (bestItem instanceof BoatItem) {
            currentContext.mlgType = MlgContext.MlgType.BOAT;
        } else {
            currentContext.mlgType = MlgContext.MlgType.SOLID;
        }

        currentPhase = MlgPhase.DEPLOYING;
        PiggyBuildClient.LOGGER.info("[AutoMLG] Targeting calculated perfectly. Transitioning into DEPLOYING.");
    }

    private void handleDeploying(Minecraft client, LocalPlayer player) {
        if (currentContext == null) return;
        
        actionQueue.enqueue(new SwapSlotAction(currentContext.mlgItemSlot));
        
        actionQueue.enqueue(new SetRotationAction(player.getYRot(), 90.0f, true));
        
        if (currentContext.mlgType == MlgContext.MlgType.SOLID) {
            net.minecraft.world.phys.BlockHitResult hitResult = is.pig.minecraft.build.lib.placement.BlockPlacer.createHitResult(currentContext.impactPos, net.minecraft.core.Direction.UP);
            actionQueue.enqueue(new UseItemOnBlockAction(net.minecraft.world.InteractionHand.MAIN_HAND, hitResult));
        } else {
            actionQueue.enqueue(new UseItemAction(net.minecraft.world.InteractionHand.MAIN_HAND));
        }
        
        currentPhase = MlgPhase.AWAITING_COLLISION;
        PiggyBuildClient.LOGGER.info("[AutoMLG] Actions enqueued. Transitioning to AWAITING_COLLISION.");
    }

    private void handleAwaitingCollision(Minecraft client, LocalPlayer player) {
        if (currentContext == null || currentContext.impactPos == null) return;
        
        BlockState impactState = client.level.getBlockState(currentContext.impactPos);
        boolean isBouncy = impactState.is(Blocks.SLIME_BLOCK) || impactState.is(Blocks.HAY_BLOCK) || impactState.getBlock() instanceof net.minecraft.world.level.block.BedBlock;
        
        if (isBouncy && player.getDeltaMovement().y > 0) {
            currentContext.isBouncing = true;
            currentPhase = MlgPhase.BOUNCING_SUSPEND;
            PiggyBuildClient.LOGGER.info("[AutoMLG] Bounce physics detected! Suspending state machine to BOUNCING_SUSPEND.");
            return;
        }

        if (currentContext.targetPos != null && actionQueue.isEmpty()) {
            boolean isMissing = false;
            BlockState checkState = client.level.getBlockState(currentContext.targetPos);
            
            if (currentContext.mlgType == MlgContext.MlgType.LIQUID) {
                if (!checkState.getFluidState().isSource()) {
                    isMissing = true;
                }
            } else if (currentContext.mlgType == MlgContext.MlgType.SOLID) {
                if (checkState.isAir() && checkState.getFluidState().isEmpty()) {
                    isMissing = true;
                }
            }

            if (isMissing && player.fallDistance > 0.0f) {
                PiggyBuildClient.LOGGER.info("[AutoMLG] Server Rollback detected! Block missing at targetPos. Forcing re-evaluation.");
                actionQueue.clearQueue();
                currentContext.failedSlots.add(currentContext.originalSlot);
                currentPhase = MlgPhase.TARGETING;
                return;
            }
        }
        
        if (player.fallDistance == 0.0f) {
            boolean blockMatched = false;
            if (currentContext.targetPos != null) {
                BlockState targetState = client.level.getBlockState(currentContext.targetPos);
                if (currentContext.mlgType == MlgContext.MlgType.LIQUID) {
                    if (targetState.getFluidState().isSource()) {
                        blockMatched = true;
                    }
                } else if (currentContext.mlgType == MlgContext.MlgType.SOLID) {
                    if (!targetState.isAir()) {
                        blockMatched = true;
                    }
                } else if (currentContext.mlgType == MlgContext.MlgType.BOAT) {
                    java.util.List<net.minecraft.world.entity.vehicle.Boat> boats = client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, new net.minecraft.world.phys.AABB(currentContext.targetPos).inflate(3.0));
                    if (!boats.isEmpty()) {
                        blockMatched = true;
                    }
                } else if (currentContext.mlgType == MlgContext.MlgType.PEARL) {
                    blockMatched = true;
                }
            }
            if (blockMatched) {
                currentPhase = MlgPhase.CLEANUP;
            }
        }
    }

    private void handleBouncingSuspend(Minecraft client, LocalPlayer player) {
        if (player.getDeltaMovement().y <= 0) {
            currentPhase = MlgPhase.IDLE;
            PiggyBuildClient.LOGGER.info("[AutoMLG] Player reached peak of bounce. Resuming IDLE sequence.");
        }
    }

    private void handleCleanup(Minecraft client, LocalPlayer player) {
        if (currentContext == null || currentContext.targetPos == null) {
            resetMlg();
            return;
        }

        if (actionQueue.isEmpty()) {
            if (currentContext.mlgType == MlgContext.MlgType.LIQUID) {
                int bucketSlot = MlgItemEvaluator.findEmptyBucket(player, currentContext.mlgItemSlot);
                if (bucketSlot != -1) {
                    actionQueue.enqueue(new SwapSlotAction(bucketSlot));
                    actionQueue.enqueue(new ScoopWaterAction(currentContext.targetPos));
                }
            } else if (currentContext.mlgType == MlgContext.MlgType.SOLID) {
                actionQueue.enqueue(new BreakBlockAction(currentContext.targetPos));
            } else if (currentContext.mlgType == MlgContext.MlgType.BOAT) {
                actionQueue.enqueue(new CleanBoatAction(currentContext.targetPos));
            }
            
            actionQueue.enqueue(new CallbackAction(() -> {
                resetMlg();
                PiggyBuildClient.LOGGER.info("[AutoMLG] Cleanup action finalized. State nullified.");
            }, 10));
        }
    }

    private void executePlacementRotations(Minecraft client, LocalPlayer player, Vec3 eyePos, Vec3 targetCenter, boolean isFluidItem) {
        double dX = targetCenter.x - eyePos.x;
        double dY = targetCenter.y - eyePos.y;
        double dZ = targetCenter.z - eyePos.z;
        double dXZ = Math.sqrt(dX * dX + dZ * dZ);
        float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
        float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

        if (currentContext != null && currentContext.mlgType == MlgContext.MlgType.PEARL && targetPitch > 84.0f) {
            targetPitch = 84.0f;
            PiggyBuildClient.LOGGER.info("[AutoMLG-Debug] Mathematically maximized pearl pitch to 84.0 to bypass velocity swept-collisions.");
        } else if (isFluidItem) {
            targetPitch = 90.0f; // Force fluid deployments perfectly downwards to guarantee raycast floor collision accuracy!
        }

        boolean isPearl = (currentContext != null && currentContext.mlgType == MlgContext.MlgType.PEARL);
        if (client.getConnection() != null && !isPearl) {
            client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(targetYaw, targetPitch, player.onGround()));
        }
        player.setYRot(targetYaw);
        player.setXRot(targetPitch);
    }

    private boolean tryAmbientIntercept(Minecraft client, LocalPlayer player, BlockPos impactPos, BlockPos placePos) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = Vec3.atBottomCenterOf(placePos);
        
        if (eyePos.distanceTo(targetCenter) <= 4.5) {
            net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(impactPos).inflate(2.0);
            for (net.minecraft.world.entity.Entity entity : client.level.getEntities(player, searchBox)) {
                if (eyePos.distanceTo(entity.position()) > 3.0) continue; // Server universally rejects entity interactions physically wider than 3.0 blocks!
                if (entity instanceof net.minecraft.world.entity.vehicle.Boat ||
                    entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) {
                    if (!entity.hasPassenger(player)) {
                        if (client.getConnection() != null) {
                            client.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(entity, false, net.minecraft.world.InteractionHand.MAIN_HAND));
                            PiggyBuildClient.LOGGER.info("[AutoMLG] Successfully intercepted ambient vehicle!");
                            return true;
                        }
                    }
                }
                if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    if (living instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) {
                        if (client.getConnection() != null) {
                            client.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(entity, false, net.minecraft.world.InteractionHand.MAIN_HAND));
                            return true;
                        }
                    }
                    if (living instanceof net.minecraft.world.entity.animal.Pig pig && pig.isSaddled()) {
                        if (client.getConnection() != null) {
                            client.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(entity, false, net.minecraft.world.InteractionHand.MAIN_HAND));
                            return true;
                        }
                    }
                    if (living instanceof net.minecraft.world.entity.monster.Strider strider && strider.isSaddled()) {
                        if (client.getConnection() != null) {
                            client.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(entity, false, net.minecraft.world.InteractionHand.MAIN_HAND));
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean isSafeBlock(BlockState state) {
        if (state.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER) || state.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_WATER)) return true;
        if (state.is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)) return true;
        if (state.is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK)) return true;
        if (state.is(net.minecraft.world.level.block.Blocks.COBWEB)) return true;
        if (state.is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)) return true;
        if (state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)) return true;
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) return true;
        if (state.is(net.minecraft.world.level.block.Blocks.LADDER) || state.is(net.minecraft.world.level.block.Blocks.VINE) || state.is(net.minecraft.world.level.block.Blocks.TWISTING_VINES)) return true;

        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) &&
            state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
            return true;
        }
        return false;
    }

    private void swapSlot(Minecraft client, int slot) {
        if (slot >= 0 && slot < 9 && client.player.getInventory().selected != slot) {
            client.player.getInventory().selected = slot;
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            }
        }
    }

    private void resetMlg() {
        Minecraft.getInstance().options.keyUse.setDown(false); // Just blindly reset it to be safe!
        currentPhase = MlgPhase.IDLE;
        currentContext = null;
        cleanupTicks = 0;
        cleanupDelayTicks = 0;
        cleanupManager.reset();
        actionQueue.clearQueue();
    }
}
