package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.lib.placement.BlockPlacer;
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

    // State tracking for MLG
    private enum MlgState { IDLE, CLEANUP, POST_CLEANUP }
    private MlgState state = MlgState.IDLE;

    private int originalSlot = -1;
    private int mlgSlot = -1;
    private BlockPos placedPos = null;
    private boolean isWater = false;
    private boolean isBoat = false;
    private boolean isPearl = false;
    private boolean isLava = false;
    private boolean isChorus = false;
    private int cleanupTicks = 0;
    private int cleanupDelayTicks = 0;
    private boolean startedDestroying = false;
    private final java.util.Set<BlockPos> breakQueue = new java.util.HashSet<>();
    private final java.util.Set<Integer> vehiclesToBreak = new java.util.HashSet<>();
    private BlockPos currentlyMining = null;

    public void onTick(Minecraft client) {
        if (InputController.autoMlgKey == null) return;

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

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            resetMlg();
            return;
        }

        processBreakQueue(client, player);
        processVehiclesQueue(client, player);

        if (player.isDeadOrDying()) {
            breakQueue.clear();
            vehiclesToBreak.clear();
        }

        if (player.fallDistance > 3.0f && player.getDeltaMovement().y < -0.5) {
            float currentDamage = player.fallDistance - 3.0f;
            if (currentDamage >= player.getHealth() || PiggyBuildConfig.getInstance().isAutoMlgAlways()) {
                int globalBestSlot = -1;
                int globalBestPriority = 999;
                for (int i = 0; i < 36; i++) {
                    net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                    if (!stack.isEmpty() && isValidMlg(stack.getItem(), player, player.fallDistance, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), BlockPos.ZERO)) {
                        int priority = getMlgPriorityRank(stack.getItem(), null);
                        if (priority < globalBestPriority) {
                            globalBestPriority = priority;
                            globalBestSlot = i;
                        }
                    }
                }

                if (globalBestSlot != -1) {
                    is.pig.minecraft.lib.ui.IconQueueOverlay.queueIcon(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("piggy", "textures/gui/icons/auto_mlg.png"),
                        1000, true
                    );

                    Item bestItem = player.getInventory().getItem(globalBestSlot).getItem();
                    if (state == MlgState.IDLE && (bestItem == Items.ENDER_PEARL || bestItem == Items.CHORUS_FRUIT)) {
                        originalSlot = player.getInventory().selected;
                        mlgSlot = globalBestSlot;
                        isPearl = bestItem == Items.ENDER_PEARL;
                        isChorus = bestItem == Items.CHORUS_FRUIT;

                        swapSlot(client, globalBestSlot);

                        if (isChorus) {
                            if (!player.isUsingItem()) {
                                client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                            }
                            client.options.keyUse.setDown(true);
                            PiggyBuildClient.LOGGER.info("[AutoMLG] Early Chorus ingestion triggered unconditionally!");
                        } else if (isPearl) {
                            float originalPitch = player.getXRot();
                            player.setXRot(90.0f);
                            if (client.getConnection() != null) {
                                client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(player.getYRot(), 90.0f, player.onGround()));
                            }
                            client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                            player.setXRot(originalPitch);
                            PiggyBuildClient.LOGGER.info("[AutoMLG] Early Pearl deployed unconditionally!");
                        }

                        state = MlgState.CLEANUP;
                        cleanupDelayTicks = 0;
                        return;
                    }
                }
            }
        }

        if (state == MlgState.POST_CLEANUP) {
            cleanupTicks++;
            if (cleanupTicks >= 3) {
                swapSlot(client, originalSlot);
                resetMlg();
            }
            return;
        }

        if (state == MlgState.IDLE) {
            handleFalling(client, player);
        } 
        
        if (state == MlgState.CLEANUP) {
            if (isBoat) {
                cleanupDelayTicks++;
                if (cleanupDelayTicks > 20) {
                    PiggyBuildClient.LOGGER.warn("[AutoMLG] Boat MLG mathematical timeout breached! Relinquishing vertical velocity lock.");
                    resetMlg();
                    return;
                }
                
                player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                
                net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(8.0);
                for (net.minecraft.world.entity.Entity entity : client.level.getEntities(player, searchBox)) {
                    if (entity instanceof net.minecraft.world.entity.vehicle.Boat boat) {
                        if (!boat.hasPassenger(player)) {
                            if (client.getConnection() != null) {
                                client.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(entity, player.isShiftKeyDown(), InteractionHand.MAIN_HAND));
                            }
                        }
                    }
                }
                
                if (player.isPassenger() && player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat) {
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Successfully intercepted and mounted Boat Native Entity.");
                    handleCleanup(client, player);
                }
            } else {
                cleanupDelayTicks++;
                int cps = is.pig.minecraft.build.config.PiggyBuildConfig.getInstance().getTickDelay();
                // ABSOLUTE Minimum 10 tick delay (0.5s) to guarantee server physics process the placed water!
                int requiredDelay = Math.max(10, (cps > 0) ? (20 / cps) : 0);
                
                if (player.fallDistance == 0.0f && cleanupDelayTicks >= requiredDelay) {
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Fall damage reset detected AND server buffer wait finished. Initializing Cleanup.");
                    handleCleanup(client, player);
                }
            }
        }
    }

    private void handleFalling(Minecraft client, LocalPlayer player) {
        if (player.fallDistance < 3.0f || player.getDeltaMovement().y >= 0) {
            return;
        }

        double startY = player.getY();
        double dist = -1;
        BlockPos impactPos = null;
        net.minecraft.world.phys.AABB baseBox = player.getBoundingBox();

        Vec3 simPos = player.position();
        Vec3 simVel = player.getDeltaMovement();
        int previousMinY = net.minecraft.util.Mth.floor(baseBox.minY);

        // Limit simulation to precisely 3 ticks to guarantee zero horizontal WASD drift 
        // which causes ghost placements on distant ledges!
        for (int ticks = 1; ticks <= 3; ticks++) { 
            simPos = simPos.add(simVel);
            simVel = new Vec3(simVel.x * 0.91, (simVel.y - 0.08) * 0.98, simVel.z * 0.91);

            net.minecraft.world.phys.AABB checkBox = baseBox.move(simPos.subtract(player.position()));
            
            int minX = net.minecraft.util.Mth.floor(checkBox.minX);
            int maxX = net.minecraft.util.Mth.floor(checkBox.maxX);
            int currentMinY = net.minecraft.util.Mth.floor(checkBox.minY);
            int minZ = net.minecraft.util.Mth.floor(checkBox.minZ);
            int maxZ = net.minecraft.util.Mth.floor(checkBox.maxZ);
            
            for (int by = previousMinY; by >= currentMinY; by--) {
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        BlockPos bp = new BlockPos(bx, by, bz);
                        BlockState bState = client.level.getBlockState(bp);
                        if (!bState.getCollisionShape(client.level, bp).isEmpty() || !bState.getFluidState().isEmpty()) {
                            if (impactPos == null || bp.getY() > impactPos.getY()) {
                                dist = startY - (bp.getY() + 1);
                                impactPos = bp;
                            }
                        }
                    }
                }
                if (impactPos != null) {
                    break;
                }
            }
            if (impactPos != null) {
                break;
            }
            previousMinY = currentMinY - 1;
        }

        if (dist == -1 || impactPos == null) {
            // Not close enough to ground
            return;
        }

        BlockPos placePos = impactPos.above();
        Vec3 eyePos = player.getEyePosition();
        Vec3 targetCenter = Vec3.atBottomCenterOf(placePos);
        
        float predictedFallDist = player.fallDistance + (float) dist;
        float predictedDamage = predictedFallDist - 3.0f;

        if (predictedDamage <= 0) return;

        if (predictedDamage < player.getHealth() && !PiggyBuildConfig.getInstance().isAutoMlgAlways()) {
            // Re-evaluate silently instead of printing massive spam if they bounce natively
            return;
        }
        
        BlockState impactState = client.level.getBlockState(impactPos);
        BlockState placeState = client.level.getBlockState(placePos);

        if (isSafeBlock(impactState) || isSafeBlock(placeState)) {
            return;
        }

        int bestSlot = findMlgItem(player, client, predictedFallDist, impactState, placePos);
        if (bestSlot == -1) {
            return;
        }

        if (eyePos.distanceTo(targetCenter) > 4.5) {
            return;
        }

        PiggyBuildClient.LOGGER.info("[AutoMLG] Danger detected! Distance to impact: {}, Predicted Damage: {}, Euclidean Reach: {}, Impact Pos: {}", dist, predictedDamage, eyePos.distanceTo(targetCenter), impactPos);

        if (eyePos.distanceTo(targetCenter) <= 4.5) {
            if (tryAmbientIntercept(client, player, impactPos, placePos)) {
                return;
            }
        }

        originalSlot = player.getInventory().selected;
        mlgSlot = bestSlot;
        isWater = player.getInventory().getItem(bestSlot).getItem() == net.minecraft.world.item.Items.WATER_BUCKET;
        isBoat = player.getInventory().getItem(bestSlot).getItem() instanceof net.minecraft.world.item.BoatItem;
        isLava = player.getInventory().getItem(bestSlot).getItem() == net.minecraft.world.item.Items.LAVA_BUCKET;
        placedPos = placePos;

        PiggyBuildClient.LOGGER.info("[AutoMLG] Target placing MLG item at {} natively onto {}", placePos, impactPos);

        swapSlot(client, bestSlot);

        for (int clearY = impactPos.getY() + 1; clearY <= Math.ceil(eyePos.y); clearY++) {
            BlockPos toClear = new BlockPos(impactPos.getX(), clearY, impactPos.getZ());
            BlockState clearState = client.level.getBlockState(toClear);
            if (!clearState.isAir() && clearState.getCollisionShape(client.level, toClear).isEmpty()) {
                client.gameMode.startDestroyBlock(toClear, net.minecraft.core.Direction.UP);
            }
        }

        PiggyBuildClient.LOGGER.info("[AutoMLG] Triggering MLG! Slot: {}, Item: {}", bestSlot, player.getInventory().getItem(bestSlot).getItem().toString());

        if (isWater || isBoat || isLava) {
            double dX = targetCenter.x - eyePos.x;
            double dY = targetCenter.y - eyePos.y;
            double dZ = targetCenter.z - eyePos.z;
            double dXZ = Math.sqrt(dX * dX + dZ * dZ);
            float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
            float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

            float originalYaw = player.getYRot();
            if (client.getConnection() != null) {
                client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(targetYaw, targetPitch, player.onGround()));
            }
            player.setYRot(targetYaw);
            player.setXRot(targetPitch);

            if (isWater && impactState != null && impactState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                is.pig.minecraft.build.lib.placement.BlockPlacer.placeBlock(impactPos, net.minecraft.core.Direction.UP, net.minecraft.world.InteractionHand.MAIN_HAND);
                PiggyBuildClient.LOGGER.info("[AutoMLG] Executed BlockPlacer.placeBlock() to bypass waterlogging on {} at Yaw {} Pitch {}", impactPos, targetYaw, targetPitch);
            } else {
                client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                PiggyBuildClient.LOGGER.info("[AutoMLG] Executed useItem() natively at true computed rotations: Yaw {} Pitch {}", targetYaw, targetPitch);
            }
            
            player.setYRot(originalYaw);
        } else {
            float originalPitch = player.getXRot();
            if (client.getConnection() != null) {
                client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(player.getYRot(), 90.0f, player.onGround()));
            }
            player.setXRot(90.0f);

            net.minecraft.world.phys.BlockHitResult hitResult = is.pig.minecraft.build.lib.placement.BlockPlacer.createHitResult(impactPos, net.minecraft.core.Direction.UP);
            if (!client.gameMode.useItemOn(player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult).consumesAction()) {
                client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(net.minecraft.world.InteractionHand.MAIN_HAND, hitResult, 0));
                PiggyBuildClient.LOGGER.info("[AutoMLG] Sent raw ServerboundUseItemOnPacket");
            } else {
                PiggyBuildClient.LOGGER.info("[AutoMLG] Executed useItemOn successfully");
            }
            player.setXRot(originalPitch);
        }
        state = MlgState.CLEANUP;
        cleanupDelayTicks = 0;
        PiggyBuildClient.LOGGER.info("[AutoMLG] State transitioned to CLEANUP.");
    }

    private void handleCleanup(Minecraft client, LocalPlayer player) {
        if (player.isDeadOrDying()) {
            PiggyBuildClient.LOGGER.info("[AutoMLG] Player died. MLG failed.");
            resetMlg();
            return;
        }

        if (player.fallDistance > 0 && !player.onGround() && !player.isInWater() && !player.isPassenger()) {
            if (isBoat && placedPos != null) {
                java.util.List<net.minecraft.world.entity.vehicle.Boat> boats = client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, new net.minecraft.world.phys.AABB(placedPos).inflate(3.0));
                for (net.minecraft.world.entity.vehicle.Boat boatEntity : boats) {
                    if (!boatEntity.hasPassenger(player)) {
                        client.gameMode.interact(player, boatEntity, net.minecraft.world.InteractionHand.MAIN_HAND);
                        PiggyBuildClient.LOGGER.info("[AutoMLG] Attempting to ride placed boat...");
                        return;
                    }
                }
            }
            return;
        }

        PiggyBuildClient.LOGGER.info("[AutoMLG] Fall damage reset detected. Initializing Cleanup.");

        if (placedPos != null) {
            if (isWater || isLava) {
                BlockPos foundWater = null;
                for (BlockPos bp : BlockPos.betweenClosed(placedPos.offset(-2, -2, -2), placedPos.offset(2, 2, 2))) {
                    if (client.level.getBlockState(bp).getFluidState().isSourceOfType(isWater ? net.minecraft.world.level.material.Fluids.WATER : net.minecraft.world.level.material.Fluids.LAVA)) {
                        foundWater = bp.immutable();
                        break;
                    }
                }

                int bucketSlot = findEmptyBucket(player);
                if (bucketSlot != -1) {
                    swapSlot(client, bucketSlot);
                    float originalPitch = player.getXRot();

                    if (foundWater != null) {
                        placedPos = foundWater;
                        Vec3 cleanupTarget = Vec3.atCenterOf(placedPos);
                        double dX = cleanupTarget.x - player.getEyePosition().x;
                        double dY = cleanupTarget.y - player.getEyePosition().y;
                        double dZ = cleanupTarget.z - player.getEyePosition().z;
                        double dXZ = Math.sqrt(dX * dX + dZ * dZ);
                        float scoopYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
                        float scoopPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

                        if (client.getConnection() != null) {
                            client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(scoopYaw, scoopPitch, player.onGround()));
                        }
                    } else {
                        if (client.getConnection() != null) {
                            client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(player.getYRot(), 90.0f, player.onGround()));
                        }
                        player.setXRot(90.0f);
                    }

                    client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    player.setXRot(originalPitch);
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Liquid payload picked up via bucket at {}.", placedPos);
                } else {
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Could not find an empty bucket to retrieve liquid.");
                }
            }
            else if (isBoat) {
                java.util.List<net.minecraft.world.entity.vehicle.Boat> boats = client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, new net.minecraft.world.phys.AABB(placedPos).inflate(3.0));
                for (net.minecraft.world.entity.vehicle.Boat boatEntity : boats) {
                    vehiclesToBreak.add(boatEntity.getId());
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Queued local Boat Entity ID {} for asynchronous organic dismantling.", boatEntity.getId());
                }
            }
            else if (!isPearl) {
                breakQueue.add(placedPos);
                PiggyBuildClient.LOGGER.info("[AutoMLG] Queued solid block at {} for destruction.", placedPos);
            }
        }

        state = MlgState.POST_CLEANUP;
        PiggyBuildClient.LOGGER.info("[AutoMLG] Cleanup sequence successfully pushed to server. Final resting position: {}", player.position());
        cleanupTicks = 0;
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

            if (player.onGround() && player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) <= 4.5) {
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

    private boolean tryAmbientIntercept(Minecraft client, LocalPlayer player, BlockPos impactPos, BlockPos placePos) {
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(impactPos).inflate(2.0);
        for (net.minecraft.world.entity.Entity entity : client.level.getEntities(player, searchBox)) {
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

        BlockState state = client.level.getBlockState(impactPos);
        BlockState placeS = client.level.getBlockState(placePos);

        BlockPos bedPos = null;
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) bedPos = impactPos;
        else if (placeS.getBlock() instanceof net.minecraft.world.level.block.BedBlock) bedPos = placePos;

        if (bedPos != null && client.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            long timeOfDay = client.level.getDayTime() % 24000;
            boolean isNight = timeOfDay >= 12541 && timeOfDay <= 23458;
            if (isNight || client.level.isThundering()) {
                net.minecraft.world.phys.BlockHitResult hitResult = is.pig.minecraft.build.lib.placement.BlockPlacer.createHitResult(bedPos, net.minecraft.core.Direction.UP);
                if (client.getConnection() != null) {
                    client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundUseItemOnPacket(net.minecraft.world.InteractionHand.MAIN_HAND, hitResult, 0));
                    PiggyBuildClient.LOGGER.info("[AutoMLG] Intercepted ambient Bed to force sleep.");
                    return true;
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

    private boolean isValidMlg(Item item, LocalPlayer player, float fallDist, BlockState impactState, BlockPos placePos) {
        if (item == net.minecraft.world.item.Items.CHORUS_FRUIT) return true;
        if (item == net.minecraft.world.item.Items.WATER_BUCKET) {
            if (player.level().dimension() == net.minecraft.world.level.Level.NETHER) return false;
            return true;
        }
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) {
            return player.getHealth() > 6.0f;
        }
        if (item == net.minecraft.world.item.Items.ENDER_PEARL) {
            return player.getHealth() > 6.0f;
        }
        if (item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET) return true;
        if (item instanceof net.minecraft.world.item.BoatItem) return true;
        if (item == net.minecraft.world.item.Items.SLIME_BLOCK) return true;
        if (item == net.minecraft.world.item.Items.COBWEB) return true;
        if (item == net.minecraft.world.item.Items.TWISTING_VINES) return true;

        if (item == net.minecraft.world.item.Items.LADDER || item == net.minecraft.world.item.Items.VINE) {
            if (placePos == null || player.level() == null) return false;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = placePos.relative(dir);
                if (player.level().getBlockState(neighbor).isFaceSturdy(player.level(), neighbor, dir.getOpposite()) || player.level().getBlockState(neighbor).isCollisionShapeFullBlock(player.level(), neighbor)) {
                    return true;
                }
            }
            return false;
        }

        if (item == net.minecraft.world.item.Items.HAY_BLOCK) {
            float reducedDamage = (fallDist * 0.2f) - 3.0f;
            return reducedDamage < 20.0f;
        }

        if (item instanceof net.minecraft.world.item.BedItem) {
            if (player.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
            float reducedDamage = (fallDist * 0.5f) - 3.0f;
            return reducedDamage < 20.0f;
        }

        return false;
    }

    private int getMlgPriorityRank(Item item, BlockState impactState) {
        if (item == net.minecraft.world.item.Items.WATER_BUCKET) {
            if (impactState != null && impactState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                return 8; // Worst priority!
            }
            return 1;
        }
        if (item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET) return 2;
        if (item instanceof net.minecraft.world.item.BoatItem) return 3;
        if (item == net.minecraft.world.item.Items.SLIME_BLOCK) return 4;
        if (item == net.minecraft.world.item.Items.COBWEB) return 5;
        if (item == net.minecraft.world.item.Items.TWISTING_VINES) return 6;
        if (item == net.minecraft.world.item.Items.HAY_BLOCK) return 7;
        if (item instanceof net.minecraft.world.item.BedItem) return 8;
        if (item == net.minecraft.world.item.Items.LADDER || item == net.minecraft.world.item.Items.VINE) return 10;
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) return 15;
        if (item == net.minecraft.world.item.Items.ENDER_PEARL) return 20;
        if (item == net.minecraft.world.item.Items.CHORUS_FRUIT) return 25;
        return 99;
    }

    private int findMlgItem(LocalPlayer player, Minecraft client, float predictedFallDist, BlockState impactState, BlockPos placePos) {
        java.util.List<Integer> validSlots = new java.util.ArrayList<>();

        for (int i = 0; i < 36; i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            Item item = stack.getItem();

            if (isValidMlg(item, player, predictedFallDist, impactState, placePos)) {
                validSlots.add(i);
            }
        }

        if (validSlots.isEmpty()) {
            return -1;
        }

        validSlots.sort(java.util.Comparator.comparingInt(slot -> getMlgPriorityRank(player.getInventory().getItem(slot).getItem(), impactState)));

        int bestSlot = validSlots.get(0);

        if (bestSlot >= 0 && bestSlot < 9) {
            return bestSlot;
        } else if (bestSlot >= 9) {
            client.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, bestSlot, 8, net.minecraft.world.inventory.ClickType.SWAP, player);
            PiggyBuildClient.LOGGER.info("[AutoMLG] Swapped MLG item from inventory slot {} to hotbar slot 8", bestSlot);
            return 8;
        }
        return -1;
    }

    private int findEmptyBucket(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
           if (player.getInventory().getItem(i).getItem() == net.minecraft.world.item.Items.BUCKET) return i;
        }
        if (mlgSlot != -1 && player.getInventory().getItem(mlgSlot).getItem() == net.minecraft.world.item.Items.BUCKET) {
            return mlgSlot;
        }
        return -1;
    }

    private void swapSlot(Minecraft client, int slot) {
        if (slot >= 0 && slot < 9 && client.player.getInventory().selected != slot) {
            client.player.getInventory().selected = slot;
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
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
            // piggy-inventory logic fallback omitted per user DRY request
            return;
        }

        if (bestSlot != -1 && bestSlot != client.player.getInventory().selected) {
            client.player.getInventory().selected = bestSlot;
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundSetCarriedItemPacket(bestSlot));
                PiggyBuildClient.LOGGER.info("[AutoMLG] Requested piggy-inventory tool-swap targeting hotbar slot {}.", bestSlot);
            }
        }
    }

    private void resetMlg() {
        if (isChorus) {
            Minecraft.getInstance().options.keyUse.setDown(false);
            PiggyBuildClient.LOGGER.info("[AutoMLG] Safely neutralized Chorus ingestion telemetry within Global Reset.");
        }
        state = MlgState.IDLE;
        originalSlot = -1;
        mlgSlot = -1;
        placedPos = null;
        isWater = false;
        isBoat = false;
        isPearl = false;
        isLava = false;
        isChorus = false;
        cleanupTicks = 0;
        cleanupDelayTicks = 0;
        startedDestroying = false;
    }
}
