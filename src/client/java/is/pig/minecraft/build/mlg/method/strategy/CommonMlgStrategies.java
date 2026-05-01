package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.api.spi.ItemDataAdapter;
import is.pig.minecraft.inventory.util.InventorySearcher;
import is.pig.minecraft.inventory.util.ItemCondition;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import is.pig.minecraft.lib.action.world.ScoopBlockAction;

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

    public static MlgViabilityStrategy requireItem(String targetItemId) {
        return (client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            ItemDataAdapter itemData = PiggyServiceRegistry.getItemDataAdapter();
            Object player = client;
            
            Object inventory = worldState.getPlayerInventory(player);
            ItemCondition condition = stack -> itemData.isItem(stack, targetItemId);
            
            Object offhand = worldState.getPlayerMainHandItem(player); // Should be offhand, add to SPI if needed
            if (condition.matches(offhand)) return true;
            
            return InventorySearcher.findSlotInHotbar(inventory, condition) != -1 ||
                   InventorySearcher.findSlotInMain(inventory, condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireItemTag(String targetTagId) {
        return (client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            ItemDataAdapter itemData = PiggyServiceRegistry.getItemDataAdapter();
            Object player = client;
            
            Object inventory = worldState.getPlayerInventory(player);
            ItemCondition condition = stack -> itemData.hasTag(stack, targetTagId);
            
            Object offhand = worldState.getPlayerMainHandItem(player); 
            if (condition.matches(offhand)) return true;
            
            return InventorySearcher.findSlotInHotbar(inventory, condition) != -1 ||
                   InventorySearcher.findSlotInMain(inventory, condition) != -1;
        };
    }

    public static MlgViabilityStrategy requireReplaceableLanding() {
        return (client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            String worldId = worldState.getCurrentWorldId();
            BlockPos landingPos = prediction.landingPos();
            
            if (!worldState.isBlockReplaceable(worldId, landingPos)) {
                return false;
            }

            BlockPos belowPos = new BlockPos(landingPos.x(), landingPos.y() - 1, landingPos.z());
            // Need a way to check if block is air/replaceable via SPI
            return !worldState.isEmpty(worldId, belowPos) && !worldState.isBlockReplaceable(worldId, belowPos);
        };
    }

    public static MlgViabilityStrategy requireWaterloggableOrReplaceableLanding() {
        return (client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            String worldId = worldState.getCurrentWorldId();
            BlockPos landingPos = prediction.landingPos();
            
            // For now, assuming isBlockReplaceable handles waterloggable check or adding it to SPI
            if (!worldState.isBlockReplaceable(worldId, landingPos)) {
                // In a real implementation, we'd check the "waterlogged" property via SPI
                return false;
            }

            BlockPos belowPos = new BlockPos(landingPos.x(), landingPos.y() - 1, landingPos.z());
            return !worldState.isEmpty(worldId, belowPos) && !worldState.isBlockReplaceable(worldId, belowPos);
        };
    }

    public static MlgViabilityStrategy notUltrawarm() {
        return (client, prediction) -> PiggyServiceRegistry.getWorldStateAdapter().isWorldUltraWarm();
    }

    public static MlgViabilityStrategy notUnsafeWaterloggable() {
        return (client, prediction) -> {
            // Complex block property check, delegating to SPI in a real scenario
            // For Phase 5, we'll return true or add a generic "isUnsafe" check to WorldStateAdapter
            return true; 
        };
    }

    public static MlgViabilityStrategy requireClearSpace(int radius) {
        return (client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            String worldId = worldState.getCurrentWorldId();
            BlockPos landingPos = prediction.landingPos();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = new BlockPos(landingPos.x() + x, landingPos.y() + 1, landingPos.z() + z);
                    if (!worldState.isEmpty(worldId, checkPos)) {
                        return false;
                    }
                }
            }
            return true;
        };
    }

    // --- PREPARATION ---

    public static int findItemSlot(Object player, String targetItemId) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        ItemDataAdapter itemData = PiggyServiceRegistry.getItemDataAdapter();
        Object inventory = worldState.getPlayerInventory(player);
        
        ItemCondition condition = stack -> itemData.isItem(stack, targetItemId);
        // Simplified check, assuming offhand/mainhand are part of inventory search or adding separate checks
        int hotbarSlot = InventorySearcher.findSlotInHotbar(inventory, condition);
        if (hotbarSlot != -1) return hotbarSlot;
        return InventorySearcher.findSlotInMain(inventory, condition);
    }

    public static MlgPreparationStrategy swapToItemAndLookDown(String expectedItemId) {
        return (queue, client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            Object player = client;
            
            int slot = findItemSlot(player, expectedItemId);
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
                    // Window slot swapping requires container ID, which we'll assume is standard or add to SPI
                    // For Phase 5, we'll keep the logic but use generic objects
                    queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build")); // Simplified for now
                }
            }

            lookDownAction(queue, client, prediction);
        };
    }

    public static MlgPreparationStrategy lookDownWithoutItemSwap() {
        return (queue, client, prediction) -> lookDownAction(queue, client, prediction);
    }

    public static void lookDownAction(PiggyActionQueue queue, Object client, FallPredictionResult prediction) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        BlockPos landingPos = prediction.landingPos();
        Vec3 target = new Vec3(landingPos.x() + 0.5, landingPos.y() - 0.5, landingPos.z() + 0.5);
        Vec3 eyePos = worldState.getPlayerEyePosition(client);
        
        double dX = target.x() - eyePos.x();
        double dY = target.y() - eyePos.y();
        double dZ = target.z() - eyePos.z();
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
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            BlockPos targetPos = prediction.landingPos();
            Vec3 target = new Vec3(targetPos.x() + 0.5, targetPos.y() + 0.5, targetPos.z() + 0.5);
            double distance = worldState.getPlayerEyePosition(client).distanceTo(target);
            double velocityY = Math.abs(worldState.getPlayerDeltaMovement(client).y());
            double dynamicReachMargin = Math.min(5.5, 4.0 + (velocityY * 0.5));
            return distance <= dynamicReachMargin;
        };
    }

    public static MlgExecutionConditionStrategy withinDistance(double blocks) {
        return (client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            BlockPos targetPos = prediction.landingPos();
            Vec3 target = new Vec3(targetPos.x() + 0.5, targetPos.y() + 0.5, targetPos.z() + 0.5);
            return worldState.getPlayerEyePosition(client).distanceTo(target) <= blocks;
        };
    }
    
    public static MlgExecutionConditionStrategy withinTicks(int ticks) {
        return (client, prediction) -> prediction.ticksToImpact() <= ticks;
    }

    // --- GENERAL EXECUTION ---

    public static MlgExecutionStrategy interactSpecificBlock(Predicate<Object> itemCondition, java.util.function.Function<FallPredictionResult, BlockPos> blockTargeter, BiPredicate<Object, BlockPos> verification) {
        return (queue, client, prediction) -> {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            BlockPos pos = blockTargeter.apply(prediction);
            BlockHitResult hitResult = is.pig.minecraft.build.lib.placement.BlockPlacer.createHitResult(pos, Direction.UP);
            
            double targetEyeY = hitResult.hitVec().y() + 4.0;
            double targetFeetY = targetEyeY - worldState.getPlayerEyeHeight(client);
            
            Vec3 playerPos = worldState.getPlayerEyePosition(client); // Eye pos, need feet pos or just offset
            if (targetFeetY <= playerPos.y()) {
                worldState.setPlayerPosition(client, new Vec3(playerPos.x(), targetFeetY, playerPos.z()));
            }

            InteractionHand hand = InteractionHand.MAIN_HAND;
            Object offhandStack = worldState.getPlayerMainHandItem(client); // Placeholder for offhand
            if (itemCondition != null && itemCondition.test(offhandStack)) {
                hand = InteractionHand.OFF_HAND;
            }
            final InteractionHand finalHand = hand;
            
            BooleanSupplier verifySuccess = () -> {
                boolean result = verification.test(client, pos);
                // In a real decoupled system, the action itself would trigger the interaction
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

    public static MlgExecutionStrategy interactBlock(Predicate<Object> itemCondition, BiPredicate<Object, BlockPos> verification) {
        return interactSpecificBlock(itemCondition, p -> p.landingPos(), verification);
    }

    public static MlgExecutionStrategy interactBlock(Predicate<Object> itemCondition, BiPredicate<Object, BlockPos> verification) {
        return interactSpecificBlock(itemCondition, p -> p.landingPos(), verification);
    }

    // --- CLEANUP ---

    public static MlgCleanupStrategy noCleanup() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Object client, FallPredictionResult prediction) {}
            
            @Override
            public boolean isFinished(Object client, FallPredictionResult prediction) { return true; }
        };
    }

    public static MlgCleanupStrategy breakBlock() {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Object client, FallPredictionResult prediction) {
                WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                BlockPos landingPos = prediction.landingPos();
                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        landingPos,
                        600,
                        null,
                        () -> new BreakBlockAction(landingPos, "piggy-build", ActionPriority.LOW),
                        () -> {
                            String worldId = worldState.getCurrentWorldId();
                            return worldState.isEmpty(worldId, landingPos);
                        },
                        c -> {
                            // Check if player is on ground and not intersecting block
                            return true; // Simplified for now
                        }
                    )
                );
            }
            @Override
            public boolean isFinished(Object client, FallPredictionResult prediction) { return true; }
        };
    }

    public static MlgCleanupStrategy scoopItem(String expectedBlockId, String filledBucketResultId) {
        return new MlgCleanupStrategy() {
            @Override
            public void queueCleanup(PiggyActionQueue queue, Object client, FallPredictionResult prediction) {
                WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
                ItemDataAdapter itemData = PiggyServiceRegistry.getItemDataAdapter();
                BlockPos targetPos = prediction.landingPos();
                is.pig.minecraft.lib.action.deferred.DeferredActionTracker.getInstance().enqueue(
                    new is.pig.minecraft.lib.action.deferred.RepeatingActionWrapper(
                        targetPos,
                        600,
                        () -> {
                            Object inventory = worldState.getPlayerInventory(client);
                            int bucketSlot = InventorySearcher.findSlotInHotbar(inventory, s -> itemData.isItem(s, "minecraft:bucket"));
                            if (bucketSlot != -1) {
                                PiggyActionQueue.getInstance().enqueue(new SelectHotbarSlotAction(bucketSlot, "piggy-build") {
                                    @Override
                                    public ActionPriority getPriority() {
                                        return ActionPriority.NORMAL;
                                    }
                                });
                            }
                        },
                        () -> new ScoopBlockAction(targetPos, is.pig.minecraft.api.InteractionHand.MAIN_HAND, "piggy-build"),
                        () -> {
                            String worldId = worldState.getCurrentWorldId();
                            // Simplified finish check
                            return worldState.isEmpty(worldId, targetPos);
                        },
                        c -> true 
                    )
                );
            }
            @Override
            public boolean isFinished(Object client, FallPredictionResult prediction) { return true; }
        };
    }
}
