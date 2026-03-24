package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.inventory.ClickWindowSlotAction;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.AttackEntityAction;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import is.pig.minecraft.lib.action.world.ScoopBlockAction;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.inventory.search.InventorySearcher;
import is.pig.minecraft.lib.inventory.search.ItemCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Common, reusable strategies for MLG method composition.
 */
public final class CommonMlgStrategies {

    private CommonMlgStrategies() {}

    // --- VIABILITY ---

    public static MlgViabilityStrategy requireItem(Item targetItem) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            ItemCondition condition = stack -> stack.getItem() == targetItem;
            if (condition.matches(client.player.getOffhandItem())) return true;
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireItemTag(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> targetTag) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            ItemCondition condition = stack -> stack.is(targetTag);
            if (condition.matches(client.player.getOffhandItem())) return true;
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireItemClass(Class<? extends net.minecraft.world.item.Item> targetClass) {
        return (client, prediction) -> {
            boolean hasItem = false;
            if (client.player != null) {
                ItemCondition condition = stack -> targetClass.isInstance(stack.getItem());
                hasItem = condition.matches(client.player.getOffhandItem()) || InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                          InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
            }
            if (!hasItem && prediction.ticksToImpact() <= 3) {
                is.pig.minecraft.lib.util.PiggyLog logger = new is.pig.minecraft.lib.util.PiggyLog("piggy-build", "Viability");
                logger.info("[MLG] Missing Required Item Class: " + targetClass.getSimpleName());
            }
            return hasItem;
        };
    }

    public static MlgViabilityStrategy requireReplaceableLanding() {
        return (client, prediction) -> {
            if (client.level == null) return false;
            BlockState landingSpace = client.level.getBlockState(prediction.landingPos());
            if (!landingSpace.canBeReplaced()) {
                return false;
            }

            BlockState floorState = client.level.getBlockState(prediction.landingPos().below());
            return !floorState.isAir() && !floorState.canBeReplaced();
        };
    }

    public static MlgViabilityStrategy notUltrawarm() {
        return (client, prediction) -> client.level != null && !client.level.dimensionType().ultraWarm();
    }

    public static MlgViabilityStrategy requireClearSpace(int radius, Class<? extends Entity> targetEntity) {
        return (client, prediction) -> {
            if (client.level == null) return false;
            BlockPos landingPos = prediction.landingPos();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
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

    public static int findItemSlot(Minecraft client, Item targetItem) {
        if (client.player == null) return -1;
        ItemCondition condition = stack -> stack.getItem() == targetItem;
        if (condition.matches(client.player.getOffhandItem())) return -2;
        if (condition.matches(client.player.getMainHandItem())) return -3;
        int hotbarSlot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
        if (hotbarSlot != -1) return hotbarSlot;
        return InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
    }

    public static MlgPreparationStrategy swapToItemAndLookDown(Item expectedItem) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            int slot = findItemSlot(client, expectedItem);
            if (slot == -1) return;

            if (slot >= 0) {
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
            }

            lookDownAction(queue, client, prediction);
        };
    }

    public static MlgPreparationStrategy swapToItemTagAndLookDown(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> expectedTag) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            int slot = -1;
            ItemCondition condition = stack -> stack.is(expectedTag);
            if (condition.matches(client.player.getOffhandItem())) slot = -2;
            else if (condition.matches(client.player.getMainHandItem())) slot = -3;
            else {
                slot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
                if (slot == -1) slot = InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
            }
            if (slot == -1) return;

            if (slot >= 0) {
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
            }

            lookDownAction(queue, client, prediction);
        };
    }

    public static MlgPreparationStrategy swapToItemClassAndLookDown(Class<? extends net.minecraft.world.item.Item> expectedClass) {
        return (queue, client, prediction) -> {
            if (client.player == null) return;
            
            ItemCondition condition = stack -> expectedClass.isInstance(stack.getItem());
            if (!condition.matches(client.player.getOffhandItem()) && !condition.matches(client.player.getMainHandItem())) {
                int slot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
                if (slot == -1) slot = InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
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
            }

            lookDownAction(queue, client, prediction);
        };
    }

    public static MlgPreparationStrategy lookDownWithoutItemSwap() {
        return (queue, client, prediction) -> lookDownAction(queue, client, prediction);
    }

    public static void lookDownAction(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
        if (client.player == null) return;
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
    }

    // --- PREPARATION TIMING ---
    
    public static MlgTickOffsetStrategy dynamicPreparation() {
        return (client, prediction) -> {
            if (client.player == null) return 15;
            double velocityY = Math.abs(client.player.getDeltaMovement().y);
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
            Vec3 target = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);
            double distance = client.player.getEyePosition().distanceTo(target);
            double velocityY = Math.abs(client.player.getDeltaMovement().y);
            double dynamicReachMargin = Math.min(5.5, 4.0 + (velocityY * 0.5));
            return distance <= dynamicReachMargin;
        };
    }

    public static MlgExecutionConditionStrategy withinDistance(double blocks) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            BlockPos targetPos = prediction.landingPos();
            Vec3 target = Vec3.atCenterOf(targetPos).add(0, 0.5, 0);
            return client.player.getEyePosition().distanceTo(target) <= blocks;
        };
    }
    
    public static MlgExecutionConditionStrategy withinTicks(int ticks) {
        return (client, prediction) -> prediction.ticksToImpact() <= ticks;
    }

    // --- GENERAL EXECUTION ---

    public static MlgExecutionStrategy interactSpecificBlock(Predicate<ItemStack> itemCondition, java.util.function.Function<FallPredictionResult, BlockPos> blockTargeter, BiPredicate<Minecraft, BlockPos> verification) {
        return (queue, client, prediction) -> {
            BlockPos pos = blockTargeter.apply(prediction);
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            Vec3 hitVec = hitResult.getLocation();
            
            double targetEyeY = hitVec.y + 4.0;
            double targetFeetY = targetEyeY - client.player.getEyeHeight();
            
            if (client.getConnection() != null && targetFeetY <= client.player.getY()) {
                client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        client.player.getX(), targetFeetY, client.player.getZ(), client.player.onGround()));
            }

            InteractionHand hand = InteractionHand.MAIN_HAND;
            if (itemCondition != null && client.player != null && itemCondition.test(client.player.getOffhandItem())) {
                hand = InteractionHand.OFF_HAND;
            }
            final InteractionHand finalHand = hand;
            
            BooleanSupplier verifySuccess = () -> {
                boolean result = verification.test(client, pos);
                if (!result && client.player != null) {
                    InteractionResult interactResult = client.gameMode.useItemOn(client.player, finalHand, hitResult);
                    if (!interactResult.consumesAction()) {
                        client.gameMode.useItem(client.player, finalHand);
                    }
                }
                return result;
            };

            lookDownAction(queue, client, prediction);

            queue.enqueue(new InteractBlockAction(hitResult, finalHand, "piggy-build", verifySuccess) {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    public static MlgExecutionStrategy interactBlock(Predicate<ItemStack> itemCondition, BiPredicate<Minecraft, BlockPos> verification) {
        return interactSpecificBlock(itemCondition, p -> p.landingPos().below(), verification);
    }

    public static is.pig.minecraft.lib.action.IAction createSpamMountAction(FallPredictionResult prediction, Predicate<ItemStack> itemCondition, Predicate<Entity> entityFilter) {
        return new is.pig.minecraft.lib.action.AbstractAction("piggy-build", ActionPriority.HIGHEST) {
            private long start = System.currentTimeMillis();
            @Override
            protected void onExecute(Minecraft c) {
                if (c.level == null || c.player == null) return;
                var entities = c.level.getEntitiesOfClass(Entity.class, new AABB(prediction.landingPos()).inflate(3), e -> entityFilter.test(e));
                if (!entities.isEmpty()) {
                    Entity target = entities.get(0);
                    double dist = c.player.getEyePosition().distanceTo(target.position());
                    if (dist > 3.0 && c.getConnection() != null) {
                        double targetEyeY = target.getY() + 3.0;
                        double targetFeetY = targetEyeY - c.player.getEyeHeight();
                        if (targetFeetY <= c.player.getY() && targetFeetY > target.getY()) {
                            c.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                                    c.player.getX(), targetFeetY, c.player.getZ(), c.player.onGround()));
                        }
                    }
                    InteractionHand hand = InteractionHand.MAIN_HAND;
                    if (itemCondition != null && itemCondition.test(c.player.getOffhandItem())) {
                        hand = InteractionHand.OFF_HAND;
                    }
                    c.gameMode.interactAt(c.player, target, new EntityHitResult(target), hand);
                    c.gameMode.interact(c.player, target, hand);
                    c.player.swing(hand);
                }
            }
            @Override
            protected Optional<Boolean> verify(Minecraft c) {
                if (c.player != null && c.player.getVehicle() != null) return Optional.of(true);
                if (System.currentTimeMillis() - start > 1500) return Optional.of(false);
                onExecute(c);
                return Optional.empty();
            }
            @Override
            public String getName() { return "Spam Mount Entity"; }
            @Override
            public boolean isClick() { return true; }
        };
    }

    public static MlgExecutionStrategy placeAndMountEntity(Predicate<ItemStack> itemCondition, Class<? extends Entity> entityClass, BiPredicate<Minecraft, BlockPos> verification) {
        return (queue, client, prediction) -> {
            interactBlock(itemCondition, verification).queueExecution(queue, client, prediction);
            queue.enqueue(createSpamMountAction(prediction, itemCondition, e -> entityClass.isInstance(e)));
        };
    }

    // --- CLEANUP ---

    public static MlgCleanupStrategy noCleanup() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {}
            
            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) { return true; }
        };
    }

    public static MlgCleanupStrategy breakBlock() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction) {
                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        prediction.landingPos(),
                        600,
                        null,
                        () -> new BreakBlockAction(prediction.landingPos(), "piggy-build", ActionPriority.LOW),
                        () -> client.level != null && client.level.isEmptyBlock(prediction.landingPos()),
                        c -> {
                            if (c.player == null) return false;
                            if (!c.player.onGround()) return false;
                            AABB extendedBlockBox = new AABB(prediction.landingPos()).expandTowards(0, 0.1, 0);
                            return !c.player.getBoundingBox().intersects(extendedBlockBox);
                        }
                    )
                );
            }
            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) { return true; }
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
                                    BlockState targetState = client.level != null ? client.level.getBlockState(targetPos) : net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
                                    int bestSlot = (Integer) getBestToolSlotMethod.invoke(handlerInstance, client, targetPos, targetState);

                                    if (bestSlot != -1 && client.player != null && bestSlot != client.player.getInventory().selected) {
                                        PiggyActionQueue.getInstance().enqueue(new SelectHotbarSlotAction(bestSlot, "piggy-build") {
                                            @Override
                                            public ActionPriority getPriority() {
                                                return ActionPriority.LOW;
                                            }
                                        });
                                    }
                                }
                            } catch (Exception t) {
                                t.printStackTrace();
                            }
                        },
                        () -> new BreakBlockAction(targetPos, "piggy-build", ActionPriority.LOW),
                        () -> client.level != null && client.level.isEmptyBlock(targetPos),
                        c -> {
                            if (c.player == null || c.level == null) return false;
                            if (c.level.getBlockState(targetPos).is(net.minecraft.world.level.block.Blocks.COBWEB)) return true; // Cobwebs trap the player
                            if (!c.player.onGround()) return false;
                            AABB extendedBlockBox = new AABB(targetPos).expandTowards(0, 0.1, 0);
                            return !c.player.getBoundingBox().intersects(extendedBlockBox);
                        }
                    )
                );
            }
            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) { return true; }
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
                            int bucketSlot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), s -> s.is(net.minecraft.world.item.Items.BUCKET));
                            if (bucketSlot != -1 && bucketSlot != client.player.getInventory().selected) {
                                PiggyActionQueue.getInstance().enqueue(new SelectHotbarSlotAction(bucketSlot, "piggy-build") {
                                    @Override
                                    public ActionPriority getPriority() {
                                        return ActionPriority.NORMAL;
                                    }
                                });
                            }
                        },
                        () -> new ScoopBlockAction(targetPos, InteractionHand.MAIN_HAND, "piggy-build"),
                        () -> {
                            if (client.level == null || client.player == null) return true;
                            if (!client.level.getBlockState(targetPos).is(expectedBlock)) return true;
                            if (client.player.getMainHandItem().is(filledBucketResult)) return true;
                            return false;
                        },
                        c -> true // Liquids cleanly scoop from mid-air
                    )
                );
            }
            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) { return true; }
        };
    }

    public static MlgCleanupStrategy attackEntityWithWeaponSwap(Class<? extends Entity> entityClass) {
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

                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        prediction.landingPos(),
                        600,
                        () -> {
                            try {
                                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("piggy-inventory")) {
                                    Entity target = entityLocator.get();
                                    if (target != null) {
                                        Class<?> handlerClass = Class.forName("is.pig.minecraft.inventory.mvc.controller.WeaponSwapHandler");
                                        Object handlerInstance = handlerClass.getDeclaredConstructor().newInstance();
                                        java.lang.reflect.Method getBestWeaponSlotMethod = handlerClass.getMethod("getBestWeaponSlot", Minecraft.class, Entity.class);
                                        int bestSlot = (Integer) getBestWeaponSlotMethod.invoke(handlerInstance, client, target);

                                        if (bestSlot != -1 && client.player != null && bestSlot != client.player.getInventory().selected) {
                                            PiggyActionQueue.getInstance().enqueue(new SelectHotbarSlotAction(bestSlot, "piggy-build") {
                                                @Override
                                                public ActionPriority getPriority() {
                                                    return ActionPriority.LOW;
                                                }
                                            });
                                        }
                                    }
                                }
                            } catch (Exception t) {
                                t.printStackTrace();
                            }
                        },
                        () -> new AttackEntityAction(entityLocator, "piggy-build", ActionPriority.NORMAL),
                        () -> entityLocator.get() == null,
                        c -> c.player != null && c.player.getVehicle() == null
                    )
                );
            }
            @Override
            public boolean isFinished(Minecraft client, FallPredictionResult prediction) {
                if (client.level == null || client.player == null) return true;
                return client.level.getEntitiesOfClass(
                        entityClass,
                        new AABB(prediction.landingPos()).inflate(2),
                        e -> true).isEmpty();
            }
        };
    }
}
