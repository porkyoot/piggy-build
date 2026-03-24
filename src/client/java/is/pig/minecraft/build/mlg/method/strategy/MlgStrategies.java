package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.inventory.ClickWindowSlotAction;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.AttackEntityAction;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.inventory.search.InventorySearcher;
import is.pig.minecraft.lib.inventory.search.ItemCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class MlgStrategies {

    private MlgStrategies() {}

    // --- UTILS ---

    // --- VIABILITY ---

    public static MlgViabilityStrategy requireItem(Item targetItem) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            ItemCondition condition = stack -> stack.getItem() == targetItem;
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireItemTag(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> targetTag) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            ItemCondition condition = stack -> stack.is(targetTag);
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireItemClass(Class<? extends net.minecraft.world.item.Item> targetClass) {
        return (client, prediction) -> {
            boolean hasItem = false;
            if (client.player != null) {
                ItemCondition condition = stack -> targetClass.isInstance(stack.getItem());
                hasItem = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                          InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
            }
            if (!hasItem && prediction.ticksToImpact() <= 3) {
                is.pig.minecraft.lib.util.PiggyLog logger = new is.pig.minecraft.lib.util.PiggyLog("piggy-build", "Viability");
                logger.info("[MLG] Missing Required Item Class: " + targetClass.getSimpleName());
                if (client.player != null) {
                    logger.info("[MLG] Inventory dump:");
                    for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack stack = client.player.getInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            String tags = stack.getTags().map(t -> t.location().toString()).collect(java.util.stream.Collectors.joining(","));
                            logger.info("[MLG] Slot " + i + ": " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + " (Tags: " + tags + ")");
                        }
                    }
                }
            }
            return hasItem;
        };
    }

    public static MlgViabilityStrategy requireReplaceableLanding() {
        return (client, prediction) -> {
            if (client.level == null) return false;
            BlockState landingSpace = client.level.getBlockState(prediction.landingPos());
            if (!landingSpace.canBeReplaced()) {
                if (prediction.ticksToImpact() <= 3) {
                    new is.pig.minecraft.lib.util.PiggyLog("piggy-build", "Viability").info("[MLG] Landing space not replaceable: " + landingSpace);
                }
                return false;
            }

            BlockState floorState = client.level.getBlockState(prediction.landingPos().below());
            boolean validFloor = !floorState.isAir() && !floorState.canBeReplaced();
            if (!validFloor && prediction.ticksToImpact() <= 3) {
                new is.pig.minecraft.lib.util.PiggyLog("piggy-build", "Viability").info("[MLG] Floor not solid organically: " + floorState);
            }
            return validFloor;
        };
    }

    public static MlgViabilityStrategy notUltrawarm() {
        return (client, prediction) -> client.level != null && !client.level.dimensionType().ultraWarm();
    }
    
    public static MlgViabilityStrategy requireClearSpace(int radius, Class<? extends Entity> targetEntity) {
        return (client, prediction) -> {
            if (client.level == null) return false;
            BlockPos landingPos = prediction.landingPos();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = landingPos.offset(x, 1, z);
                    if (!client.level.getBlockState(checkPos).getCollisionShape(client.level, checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    // --- PREPARATION ---

    private static int findItemSlot(Minecraft client, Item targetItem) {
        if (client.player == null) return -1;
        ItemCondition condition = stack -> stack.getItem() == targetItem;
        int hotbarSlot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
        if (hotbarSlot != -1) return hotbarSlot;
        return InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
    }

    public static MlgPreparationStrategy swapToItemAndLookDown(Item expectedItem) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            int slot = findItemSlot(client, expectedItem);
            if (slot == -1) return;

            if (slot < 9) {
                queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            } else {
                int currentHotbarSlot = client.player.getInventory().selected;
                queue.enqueue(new ClickWindowSlotAction(
                        client.player.inventoryMenu.containerId,
                        slot,
                        currentHotbarSlot,
                        ClickType.SWAP,
                        "piggy-build",
                        ActionPriority.HIGHEST
                ) {
                    @Override
                    protected Optional<Boolean> verify(Minecraft client) {
                        if (client.player != null) {
                            ItemStack stack = client.player.getInventory().getItem(currentHotbarSlot);
                            if (stack.getItem() == expectedItem) {
                                return Optional.of(true);
                            }
                        }
                        return Optional.empty();
                    }
                });
            }

            Vec3 target = Vec3.atCenterOf(prediction.landingPos().below()).add(0, 0.5, 0);
            Vec3 eyePos = client.player.getEyePosition();
            double dX = target.x - eyePos.x;
            double dY = target.y - eyePos.y;
            double dZ = target.z - eyePos.z;
            double dXZ = Math.sqrt(dX * dX + dZ * dZ);
            float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
            float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

            queue.enqueue(new SetRotationAction(targetPitch, targetYaw, 0, "piggy-build") {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    private static int findItemSlotByTag(Minecraft client, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> targetTag) {
        if (client.player == null) return -1;
        ItemCondition condition = stack -> stack.is(targetTag);
        int hotbarSlot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
        if (hotbarSlot != -1) return hotbarSlot;
        return InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
    }

    public static MlgPreparationStrategy swapToItemTagAndLookDown(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> expectedTag) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            int slot = findItemSlotByTag(client, expectedTag);
            if (slot == -1) return;

            if (slot < 9) {
                queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            } else {
                int currentHotbarSlot = client.player.getInventory().selected;
                queue.enqueue(new ClickWindowSlotAction(
                        client.player.inventoryMenu.containerId,
                        slot,
                        currentHotbarSlot,
                        ClickType.SWAP,
                        "piggy-build",
                        ActionPriority.HIGHEST
                ) {
                    @Override
                    protected Optional<Boolean> verify(Minecraft client) {
                        if (client.player != null) {
                            ItemStack stack = client.player.getInventory().getItem(currentHotbarSlot);
                            if (stack.is(expectedTag)) {
                                return Optional.of(true);
                            }
                        }
                        return Optional.empty();
                    }
                });
            }

            Vec3 target = Vec3.atCenterOf(prediction.landingPos().below()).add(0, 0.5, 0);
            Vec3 eyePos = client.player.getEyePosition();
            double dX = target.x - eyePos.x;
            double dY = target.y - eyePos.y;
            double dZ = target.z - eyePos.z;
            double dXZ = Math.sqrt(dX * dX + dZ * dZ);
            float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
            float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

            queue.enqueue(new SetRotationAction(targetPitch, targetYaw, 0, "piggy-build") {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    public static MlgPreparationStrategy swapToItemClassAndLookDown(Class<? extends net.minecraft.world.item.Item> expectedClass) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            ItemCondition condition = stack -> expectedClass.isInstance(stack.getItem());
            int slot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
            if (slot == -1) {
                slot = InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
            }
            if (slot == -1) return;

            if (slot < 9) {
                queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            } else {
                int currentHotbarSlot = client.player.getInventory().selected;
                queue.enqueue(new ClickWindowSlotAction(
                        client.player.inventoryMenu.containerId,
                        slot,
                        currentHotbarSlot,
                        ClickType.SWAP,
                        "piggy-build",
                        ActionPriority.HIGHEST
                ) {
                    @Override
                    protected Optional<Boolean> verify(Minecraft client) {
                        if (client.player != null) {
                            ItemStack stack = client.player.getInventory().getItem(currentHotbarSlot);
                            if (expectedClass.isInstance(stack.getItem())) {
                                return Optional.of(true);
                            }
                        }
                        return Optional.empty();
                    }
                });
            }

            Vec3 target = Vec3.atCenterOf(prediction.landingPos().below()).add(0, 0.5, 0);
            Vec3 eyePos = client.player.getEyePosition();
            double dX = target.x - eyePos.x;
            double dY = target.y - eyePos.y;
            double dZ = target.z - eyePos.z;
            double dXZ = Math.sqrt(dX * dX + dZ * dZ);
            float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
            float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

            queue.enqueue(new SetRotationAction(targetPitch, targetYaw, 0, "piggy-build") {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    // --- PREPARATION TIMING ---
    
    public static MlgTickOffsetStrategy dynamicPreparation() {
        return (client, prediction) -> {
            if (client.player == null) return 15;
            double velocityY = Math.abs(client.player.getDeltaMovement().y);
            // Dynamic scale: Base 10 ticks + up to 20 velocity ticks + up to ~10 distance ticks.
            int offset = (int) (10 + (velocityY * 5.0) + (prediction.fallDistance() * 0.05));
            return Math.min(30, Math.max(10, offset));
        };
    }

    public static MlgTickOffsetStrategy fixedPreparationTicks(int ticks) {
        return (client, prediction) -> ticks;
    }

    // --- EXECUTION CONDITIONS ---

    public static MlgExecutionConditionStrategy dynamicReach() {
        return (client, prediction) -> {
            if (client.player == null) return false;
            BlockPos targetPos = prediction.landingPos();
            
            // Interaction face natively at the top-center of the target block
            Vec3 target = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);
            double distance = client.player.getEyePosition().distanceTo(target);
            
            double velocityY = Math.abs(client.player.getDeltaMovement().y);
            
            // Server strict interactive bounds scale rigidly up to ~4.5 blocks.
            // When falling slowly, deploy explicitly at 4.0 blocks to strictly pass server survival audits.
            // When intercepting physics at terminal speeds (3.9 blocks/tick), deploy earlier (5.5) completely
            // overriding interpolation single-tick frame loss assuring actions broadcast before impact arrays calculate.
            double dynamicReachMargin = Math.min(5.5, 4.0 + (velocityY * 0.5));
            
            return distance <= dynamicReachMargin;
        };
    }

    public static MlgExecutionConditionStrategy withinDistance(double blocks) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            BlockPos targetPos = prediction.landingPos();
            
            // Calculate distance to the top-center of the block we're targeting (ground level + 0.5y)
            Vec3 target = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);
            return client.player.getEyePosition().distanceTo(target) <= blocks;
        };
    }
    
    public static MlgExecutionConditionStrategy withinTicks(int ticks) {
        return (client, prediction) -> prediction.ticksToImpact() <= ticks;
    }

    // --- EXECUTION ---

    public static MlgExecutionStrategy interactBlock(BiPredicate<Minecraft, BlockPos> verification) {
        return (queue, client, prediction) -> {
            is.pig.minecraft.lib.util.PiggyLog logger = new is.pig.minecraft.lib.util.PiggyLog("piggy-build", "MlgExecution");
            BlockPos pos = prediction.landingPos().below();
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            Vec3 hitVec = hitResult.getLocation();
            double hitVecDist = client.player.getEyePosition().distanceTo(hitVec);
            
            logger.info("[MLG EXEC] ---- Executing Interaction! ----");
            logger.info(String.format("[MLG EXEC] Target Block: %s, Computed HitVec Dist: %.2f", pos, hitVecDist));
            
            double targetEyeY = hitVec.y + 4.0;
            double targetFeetY = targetEyeY - client.player.getEyeHeight();
            
            if (client.getConnection() != null && targetFeetY <= client.player.getY()) {
                logger.info(String.format("[MLG EXEC] Interpolating Physics! Injecting intermediate Pos Packet at Y: %.2f", targetFeetY));
                client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        client.player.getX(), targetFeetY, client.player.getZ(), client.player.onGround()));
            }

            if (client.player != null) {
                logger.info(String.format("[MLG EXEC] Main Hand: %s", BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem())));
            }
            
            BooleanSupplier verifySuccess = () -> {
                boolean result = verification.test(client, pos);
                if (!result && client.player != null) {
                    InteractionResult interactResult = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult);
                    if (!interactResult.consumesAction()) {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    }
                }
                return result;
            };

            Vec3 target = Vec3.atCenterOf(pos).add(0, 0.5, 0);
            if (client.player != null) {
                Vec3 eyePos = client.player.getEyePosition();
                double dX = target.x - eyePos.x;
                double dY = target.y - eyePos.y;
                double dZ = target.z - eyePos.z;
                double dXZ = Math.sqrt(dX * dX + dZ * dZ);
                float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
                float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

                queue.enqueue(new SetRotationAction(targetPitch, targetYaw, 0, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            }

            queue.enqueue(new InteractBlockAction(hitResult, InteractionHand.MAIN_HAND, "piggy-build", verifySuccess) {
                @Override
                protected void onExecute(Minecraft client) {
                    is.pig.minecraft.lib.util.PiggyLog logger = new is.pig.minecraft.lib.util.PiggyLog("piggy-build", "MlgExecution");
                    if (client.player != null) {
                        double dist = client.player.getEyePosition().distanceTo(hitResult.getLocation());
                        String itemStr = client.player.getMainHandItem() != null && !client.player.getMainHandItem().isEmpty() ? client.player.getMainHandItem().getItem().toString() : "None";
                        logger.info("[MLG EXEC] ---- Executing Interaction! ----");
                        logger.info("[MLG EXEC] Target Block: {}, Computed HitVec Dist: {}", pos, String.format("%.2f", dist));
                        logger.info("[MLG EXEC] Main Hand: {}", itemStr);
                        logger.info("[MLG EXEC] Player Pos: {} | Velocity: {}", client.player.position(), client.player.getDeltaMovement());
                        logger.info("[MLG EXEC] Pitch: {}, Yaw: {}", client.player.getXRot(), client.player.getYRot());
                    }
                    super.onExecute(client);
                }

                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    public static MlgExecutionStrategy interactBlockSneaking(BiPredicate<Minecraft, BlockPos> verification) {
        return (queue, client, prediction) -> {
            BlockPos pos = prediction.landingPos().below();
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            Vec3 hitVec = hitResult.getLocation();
            
            double targetEyeY = hitVec.y + 4.0;
            double targetFeetY = targetEyeY - client.player.getEyeHeight();
            
            if (client.getConnection() != null && targetFeetY <= client.player.getY()) {
                client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        client.player.getX(), targetFeetY, client.player.getZ(), client.player.onGround()));
            }

            BooleanSupplier verifySuccess = () -> {
                boolean result = verification.test(client, pos);
                if (!result && client.player != null) {
                    InteractionResult interactResult = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hitResult);
                    if (!interactResult.consumesAction()) {
                        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                    }
                }
                return result;
            };

            Vec3 target = Vec3.atCenterOf(pos).add(0, 0.5, 0);
            if (client.player != null) {
                Vec3 eyePos = client.player.getEyePosition();
                double dX = target.x - eyePos.x;
                double dY = target.y - eyePos.y;
                double dZ = target.z - eyePos.z;
                double dXZ = Math.sqrt(dX * dX + dZ * dZ);
                float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
                float targetPitch = (float) -(Math.atan2(dY, dXZ) * (180.0 / Math.PI));

                queue.enqueue(new SetRotationAction(targetPitch, targetYaw, 0, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            }

            queue.enqueue(new InteractBlockAction(hitResult, InteractionHand.MAIN_HAND, "piggy-build", verifySuccess) {
                @Override
                protected void onExecute(Minecraft client) {
                    if (client.getConnection() != null && client.player != null) {
                        client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                                client.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
                        
                        boolean wasSneaking = client.player.isShiftKeyDown();
                        client.player.setShiftKeyDown(true);
                        
                        super.onExecute(client);
                        
                        client.player.setShiftKeyDown(wasSneaking);
                        client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                                client.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
                    } else {
                        super.onExecute(client);
                    }
                }

                @Override
                protected Optional<Boolean> verify(Minecraft client) {
                    if (client.getConnection() != null && client.player != null) {
                        client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                                client.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
                        boolean wasSneaking = client.player.isShiftKeyDown();
                        client.player.setShiftKeyDown(true);
                        
                        Optional<Boolean> res = super.verify(client);
                        
                        client.player.setShiftKeyDown(wasSneaking);
                        client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(
                                client.player, net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
                        return res;
                    }
                    return super.verify(client);
                }

                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    // --- CLEANUP ---

    public static MlgCleanupStrategy breakBlock() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        prediction.landingPos(),
                        600,
                        null,
                        () -> new is.pig.minecraft.lib.action.world.BreakBlockAction(prediction.landingPos(), "piggy-build", is.pig.minecraft.lib.action.ActionPriority.LOW),
                        () -> client.level != null && client.level.isEmptyBlock(prediction.landingPos()),
                        c -> {
                            if (c.player == null) return false;
                            if (!c.player.onGround()) return false;
                            net.minecraft.world.phys.AABB extendedBlockBox = new net.minecraft.world.phys.AABB(prediction.landingPos()).expandTowards(0, 0.1, 0);
                            return !c.player.getBoundingBox().intersects(extendedBlockBox);
                        }
                    )
                );
            }

            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                return true;
            }
        };
    }

    public static MlgCleanupStrategy breakBlockWithToolSwap(Item toolTarget) {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                BlockPos targetPos = prediction.landingPos();
                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        targetPos,
                        600,
                        () -> {
                            try {
                                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("piggy-inventory")) {
                                    Class<?> handlerClass = Class.forName("is.pig.minecraft.inventory.mvc.controller.ToolSwapHandler");
                                    Object handlerInstance = handlerClass.getDeclaredConstructor().newInstance();
                                    java.lang.reflect.Method getBestToolSlotMethod = handlerClass.getMethod("getBestToolSlot", Minecraft.class, BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
                                    net.minecraft.world.level.block.state.BlockState targetState = client.level != null ? client.level.getBlockState(targetPos) : net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
                                    int bestSlot = (Integer) getBestToolSlotMethod.invoke(handlerInstance, client, targetPos, targetState);

                                    if (bestSlot != -1 && client.player != null && bestSlot != client.player.getInventory().selected) {
                                        is.pig.minecraft.lib.action.PiggyActionQueue.getInstance().enqueue(new is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction(bestSlot, "piggy-build") {
                                            @Override
                                            public is.pig.minecraft.lib.action.ActionPriority getPriority() {
                                                return is.pig.minecraft.lib.action.ActionPriority.LOW;
                                            }
                                        });
                                    }
                                }
                            } catch (Exception t) {
                                t.printStackTrace();
                            }
                        },
                        () -> new is.pig.minecraft.lib.action.world.BreakBlockAction(targetPos, "piggy-build", is.pig.minecraft.lib.action.ActionPriority.LOW),
                        () -> client.level != null && client.level.isEmptyBlock(targetPos),
                        c -> {
                            if (c.player == null || c.level == null) return false;
                            if (c.level.getBlockState(targetPos).is(net.minecraft.world.level.block.Blocks.COBWEB)) return true; // Cobwebs trap the player natively!
                            
                            if (!c.player.onGround()) return false;
                            net.minecraft.world.phys.AABB extendedBlockBox = new net.minecraft.world.phys.AABB(targetPos).expandTowards(0, 0.1, 0);
                            return !c.player.getBoundingBox().intersects(extendedBlockBox);
                        }
                    )
                );
            }

            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                return true;
            }
        };
    }

    public static MlgCleanupStrategy scoopItem(net.minecraft.world.level.block.Block expectedBlock, Item filledBucketResult) {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                BlockPos targetPos = prediction.landingPos();
                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        targetPos,
                        600,
                        () -> {
                            if (client.player == null) return;
                            int bucketSlot = is.pig.minecraft.lib.inventory.search.InventorySearcher.findSlotInHotbar(client.player.getInventory(), s -> s.is(net.minecraft.world.item.Items.BUCKET));
                            if (bucketSlot != -1 && bucketSlot != client.player.getInventory().selected) {
                                is.pig.minecraft.lib.action.PiggyActionQueue.getInstance().enqueue(new is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction(bucketSlot, "piggy-build") {
                                    @Override
                                    public is.pig.minecraft.lib.action.ActionPriority getPriority() {
                                        return is.pig.minecraft.lib.action.ActionPriority.NORMAL;
                                    }
                                });
                            }
                        },
                        () -> new is.pig.minecraft.lib.action.world.ScoopBlockAction(targetPos, net.minecraft.world.InteractionHand.MAIN_HAND, "piggy-build"),
                        () -> {
                            if (client.level == null || client.player == null) return true;
                            if (!client.level.getBlockState(targetPos).is(expectedBlock)) return true;
                            if (client.player.getMainHandItem().is(filledBucketResult)) return true;
                            return false;
                        },
                        c -> true // Liquids cleanly scoop from mid-air organically!
                    )
                );
            }

            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                return true;
            }
        };
    }

    public static MlgCleanupStrategy attackEntity(Class<? extends Entity> entityClass) {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                Supplier<Entity> entityLocator = () -> {
                    if (client.level == null || client.player == null) return null;
                    List<? extends Entity> entities = client.level.getEntitiesOfClass(
                            entityClass,
                            client.player.getBoundingBox().inflate(4),
                            e -> true);
                    if (entities.isEmpty()) return null;
                    
                    Entity closest = null;
                    double minDistance = Double.MAX_VALUE;
                    for (Entity e : entities) {
                        double dist = e.distanceToSqr(client.player);
                        if (dist < minDistance) {
                            minDistance = dist;
                            closest = e;
                        }
                    }
                    return closest;
                };

                queue.enqueue(new AttackEntityAction(entityLocator, "piggy-build", ActionPriority.NORMAL));
            }

            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                if (client.level == null || client.player == null) return true;
                return client.level.getEntitiesOfClass(
                        entityClass,
                        new net.minecraft.world.phys.AABB(prediction.landingPos()).inflate(2),
                        e -> true).isEmpty();
            }
        };
    }

    // --- CHORUS FRUIT STRATEGIES ---

    public static MlgViabilityStrategy requireTicksToImpactGreaterThan(int ticks) {
        return (client, prediction) -> prediction.ticksToImpact() > ticks;
    }

    public static MlgPreparationStrategy swapToItem(Item expectedItem) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            int slot = findItemSlot(client, expectedItem);
            if (slot == -1) return;

            if (slot < 9) {
                queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.HIGHEST;
                    }
                });
            } else {
                int currentHotbarSlot = client.player.getInventory().selected;
                queue.enqueue(new ClickWindowSlotAction(
                        client.player.inventoryMenu.containerId,
                        slot,
                        currentHotbarSlot,
                        ClickType.SWAP,
                        "piggy-build",
                        ActionPriority.HIGHEST
                ) {
                    @Override
                    protected Optional<Boolean> verify(Minecraft client) {
                        if (client.player != null) {
                            ItemStack stack = client.player.getInventory().getItem(currentHotbarSlot);
                            if (stack.getItem() == expectedItem) {
                                return Optional.of(true);
                            }
                        }
                        return Optional.empty();
                    }
                });
            }
        };
    }

    public static MlgExecutionStrategy holdUseItem(boolean state) {
        return (queue, client, prediction) -> {
            if (state) {
                queue.enqueue(new is.pig.minecraft.lib.action.world.ConsumeItemAction(InteractionHand.MAIN_HAND, "piggy-build", ActionPriority.HIGHEST));
            }
        };
    }

    public static MlgCleanupStrategy releaseUseItem() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                // ConsumeItemAction releases it automatically when done, but just in case, we send a manual release
                queue.enqueue(new is.pig.minecraft.lib.action.player.HoldKeyAction(client.options.keyUse, false, "piggy-build") {
                    @Override
                    public ActionPriority getPriority() {
                        return ActionPriority.NORMAL;
                    }
                });
            }

            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                // Key releasing events conclude unconditionally physically upon Action queue execution!
                // Wait for the release action to perfectly drain from the queue before reporting finished natively.
                return !PiggyActionQueue.getInstance().hasActions("piggy-build");
            }
        };
    }
}
