package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.inventory.ClickWindowSlotAction;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.AttackEntityAction;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import is.pig.minecraft.lib.action.world.UseItemAction;
import is.pig.minecraft.lib.inventory.search.InventorySearcher;
import is.pig.minecraft.lib.inventory.search.ItemCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    // --- VIABILITY ---

    public static MlgViabilityStrategy requireItem(Item targetItem) {
        return (client, prediction) -> {
            if (client.player == null) return false;
            ItemCondition condition = stack -> stack.getItem() == targetItem;
            return InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition) != -1 ||
                   InventorySearcher.findSlotInMain(client.player.getInventory(), condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireReplaceableLanding() {
        return (client, prediction) -> {
            if (client.level == null) return false;
            BlockState landingSpace = client.level.getBlockState(prediction.landingPos());
            if (!landingSpace.canBeReplaced()) return false;

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

    // --- EXECUTION CONDITIONS ---

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
            BlockPos pos = prediction.landingPos().below();
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
            
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

            // Rotation 2: Micro-adjustment right before interaction
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

    // --- CLEANUP ---

    public static MlgCleanupStrategy breakBlock() {
        return (queue, client, prediction) -> {
            BlockPos targetPos = prediction.landingPos();
            queue.enqueue(new BreakBlockAction(targetPos, "piggy-build", ActionPriority.NORMAL));
        };
    }

    public static MlgCleanupStrategy breakBlockWithToolSwap(Item toolTarget) {
        return (queue, client, prediction) -> {
            BlockPos targetPos = prediction.landingPos();
            
            try {
                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("piggy-inventory")) {
                    Class<?> handlerClass = Class.forName("is.pig.minecraft.inventory.mvc.controller.ToolSwapHandler");
                    Object handlerInstance = handlerClass.getDeclaredConstructor().newInstance();
                    java.lang.reflect.Method getBestToolSlotMethod = handlerClass.getMethod("getBestToolSlot", Minecraft.class, BlockPos.class, BlockState.class);
                    BlockState targetState = client.level != null ? client.level.getBlockState(targetPos) : net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState();
                    int bestSlot = (Integer) getBestToolSlotMethod.invoke(handlerInstance, client, targetPos, targetState);

                    if (bestSlot != -1 && client.player != null && bestSlot != client.player.getInventory().selected) {
                        queue.enqueue(new SelectHotbarSlotAction(bestSlot, "piggy-build") {
                            @Override
                            public ActionPriority getPriority() {
                                return ActionPriority.NORMAL;
                            }
                        });
                    }
                }
            } catch (Exception t) {
                // Silently fallback if piggy-inventory isn't linked/available successfully
            }

            queue.enqueue(new BreakBlockAction(targetPos, "piggy-build", ActionPriority.NORMAL));
        };
    }

    public static MlgCleanupStrategy scoopItem() {
        return (queue, client, prediction) -> {
            BooleanSupplier verifyScooped = () -> client.level != null && client.level.getBlockState(prediction.landingPos()).isAir();
            
            queue.enqueue(new UseItemAction(InteractionHand.MAIN_HAND, "piggy-build", verifyScooped) {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.NORMAL;
                }
            });
        };
    }

    public static MlgCleanupStrategy attackEntity(Class<? extends Entity> entityClass) {
        return (queue, client, prediction) -> {
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

            queue.enqueue(new AttackEntityAction(entityLocator, "piggy-build", ActionPriority.HIGHEST));
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
            queue.enqueue(new is.pig.minecraft.lib.action.player.HoldKeyAction(client.options.keyUse, state, "piggy-build") {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }

    public static MlgCleanupStrategy releaseUseItem() {
        return (queue, client, prediction) -> {
            queue.enqueue(new is.pig.minecraft.lib.action.player.HoldKeyAction(client.options.keyUse, false, "piggy-build") {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        };
    }
}
