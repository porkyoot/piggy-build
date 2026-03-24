package is.pig.minecraft.build.mlg.statemachine;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethodSelector;
import is.pig.minecraft.build.mlg.method.strategy.MlgStrategies;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public class MlgStateMachine {

    private static final MlgStateMachine INSTANCE = new MlgStateMachine();
    private static final PiggyLog LOGGER = new PiggyLog("piggy-build", "MlgStateMachine");

    private static final List<ComposedMlgMethod> METHODS = List.of(
        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(99)
            .cleanupDifficulty(0)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireExistingMountableEntity())
            .preparation(MlgStrategies.lookDownWithoutItemSwap())
            .execution(MlgStrategies.mountExistingEntity())
            .cleanup(MlgStrategies.noCleanup())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(98)
            .cleanupDifficulty(3)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireRailAndMinecart())
            .preparation(MlgStrategies.swapToItemClassAndLookDown(net.minecraft.world.item.MinecartItem.class))
            .execution(MlgStrategies.placeMinecartAndMountEntity(stack -> stack.getItem() instanceof net.minecraft.world.item.MinecartItem, net.minecraft.world.entity.vehicle.AbstractMinecart.class, (client, pos) -> client.level != null && !client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.AbstractMinecart.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(MlgStrategies.attackEntityWithWeaponSwap(net.minecraft.world.entity.vehicle.AbstractMinecart.class))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(90)
            .cleanupDifficulty(0)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireSaddleAndUnsaddledAnimal())
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.SADDLE))
            .execution(MlgStrategies.saddleAndMountEntity())
            .cleanup(MlgStrategies.noCleanup())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(99)
            .cleanupDifficulty(0)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireExistingMountableEntity())
            .preparation(MlgStrategies.lookDownWithoutItemSwap())
            .execution(MlgStrategies.mountExistingEntity())
            .cleanup(MlgStrategies.noCleanup())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(100)
            .cleanupDifficulty(1)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.WATER_BUCKET)
                .and(MlgStrategies.requireReplaceableLanding())
                .and(MlgStrategies.notUltrawarm()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.WATER_BUCKET))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.WATER_BUCKET), (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)))
            .cleanup(MlgStrategies.scoopItem(net.minecraft.world.level.block.Blocks.WATER, net.minecraft.world.item.Items.WATER_BUCKET))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(95)
            .cleanupDifficulty(3)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.SLIME_BLOCK)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.SLIME_BLOCK))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.SLIME_BLOCK), (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK)))
            .cleanup(MlgStrategies.breakBlock())
            .requiresBounceSettlement(false)
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(90)
            .cleanupDifficulty(5)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.COBWEB)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.COBWEB))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.COBWEB), (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.COBWEB)))
            .cleanup(MlgStrategies.breakBlockWithToolSwap(net.minecraft.world.item.Items.COBWEB))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(4)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItemTag(net.minecraft.tags.ItemTags.BOATS)
                .and(MlgStrategies.requireReplaceableLanding())
                .and(MlgStrategies.requireClearSpace(1, net.minecraft.world.entity.vehicle.Boat.class)))
            .preparation(MlgStrategies.swapToItemTagAndLookDown(net.minecraft.tags.ItemTags.BOATS))
            .execution(MlgStrategies.placeAndMountEntity(stack -> stack.is(net.minecraft.tags.ItemTags.BOATS), net.minecraft.world.entity.vehicle.Boat.class, (client, pos) -> client.level != null && !client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(MlgStrategies.attackEntityWithWeaponSwap(net.minecraft.world.entity.vehicle.Boat.class))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(4)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItemTag(net.minecraft.tags.ItemTags.CHEST_BOATS)
                .and(MlgStrategies.requireReplaceableLanding())
                .and(MlgStrategies.requireClearSpace(1, net.minecraft.world.entity.vehicle.ChestBoat.class)))
            .preparation(MlgStrategies.swapToItemTagAndLookDown(net.minecraft.tags.ItemTags.CHEST_BOATS))
            .execution(MlgStrategies.placeAndMountEntity(stack -> stack.is(net.minecraft.tags.ItemTags.CHEST_BOATS), net.minecraft.world.entity.vehicle.ChestBoat.class, (client, pos) -> client.level != null && !client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.ChestBoat.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(MlgStrategies.attackEntityWithWeaponSwap(net.minecraft.world.entity.vehicle.ChestBoat.class))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(1)
            .isPositionDependent(false)
            .preparationTickOffset(MlgStrategies.fixedPreparationTicks(1000)) // Allows this to instantly prepare out of logic loop
            .executionCondition(MlgStrategies.withinTicks(1000)) // Instantly start executing
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.CHORUS_FRUIT)
                .and(MlgStrategies.requireTicksToImpactGreaterThan(35))) // It physically takes 32 ticks to eat food
            .preparation(MlgStrategies.swapToItem(net.minecraft.world.item.Items.CHORUS_FRUIT))
            .execution(MlgStrategies.holdUseItem(stack -> stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT), true))
            .cleanup(MlgStrategies.releaseUseItem())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(30) // Low reliability due to RNG
            .cleanupDifficulty(2)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.BONE_MEAL)
                .and((client, prediction) -> client.level != null && client.level.getBlockState(prediction.landingPos().below()).is(net.minecraft.world.level.block.Blocks.WARPED_NYLIUM))
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.BONE_MEAL))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.BONE_MEAL), (client, pos) -> client.level != null && (client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.TWISTING_VINES) || client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.TWISTING_VINES_PLANT))))
            .cleanup(MlgStrategies.breakBlock())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true) // Stops the physical fall natively via teleportation
            .reliabilityScore(85)
            .cleanupDifficulty(1)
            .selfDamage(5.0f) // Ender Pearls inflict 5 points of raw Fall Damage intrinsically!
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.withinTicks(7)) // Throw natively a fraction of a second prior to impact
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.ENDER_PEARL))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.ENDER_PEARL))
            .execution((queue, client, prediction) -> {
                net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
                if (client.player != null && client.player.getOffhandItem().is(net.minecraft.world.item.Items.ENDER_PEARL)) {
                    hand = net.minecraft.world.InteractionHand.OFF_HAND;
                }
                queue.enqueue(new is.pig.minecraft.lib.action.world.UseItemAction(
                        hand,
                        "piggy-build",
                        () -> true // Verified via physical inventory consumption and velocity changes intrinsically
                ) {
                    @Override
                    public is.pig.minecraft.lib.action.ActionPriority getPriority() {
                        return is.pig.minecraft.lib.action.ActionPriority.HIGHEST;
                    }
                });
            })
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(false) // Hay Bales do NOT negate all damage natively!
            .fallDamageMultiplier(0.2f) // Hay bales reduce fall damage by 80%
            .reliabilityScore(85)
            .cleanupDifficulty(3)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.HAY_BLOCK)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.HAY_BLOCK))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.HAY_BLOCK), (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)))
            .cleanup(MlgStrategies.breakBlock())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(false) // Beds do NOT negate all damage natively!
            .fallDamageMultiplier(0.5f) // Beds reduce fall damage by 50%
            .reliabilityScore(70)
            .cleanupDifficulty(5)
            .requiresBounceSettlement(false)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItemClass(net.minecraft.world.item.BedItem.class)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemClassAndLookDown(net.minecraft.world.item.BedItem.class))
            .execution((queue, client, prediction) -> {
                MlgStrategies.interactBlock(stack -> stack.getItem() instanceof net.minecraft.world.item.BedItem, (c, pos) -> c.level != null && c.level.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.BEDS)).queueExecution(queue, client, prediction);
                queue.enqueue(new is.pig.minecraft.lib.action.player.HoldKeyAction(client.options.keyUse, false, "piggy-build") {
                    @Override
                    public is.pig.minecraft.lib.action.ActionPriority getPriority() {
                        return is.pig.minecraft.lib.action.ActionPriority.HIGHEST;
                    }
                });
            })
            .cleanup(MlgStrategies.breakBlockWithToolSwap(net.minecraft.world.item.Items.WOODEN_AXE))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(98)
            .cleanupDifficulty(3)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.POWDER_SNOW_BUCKET), (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)))
            .cleanup(MlgStrategies.scoopItem(net.minecraft.world.level.block.Blocks.POWDER_SNOW, net.minecraft.world.item.Items.POWDER_SNOW_BUCKET))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(85)
            .cleanupDifficulty(3)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.TWISTING_VINES)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.TWISTING_VINES))
            .execution(MlgStrategies.interactBlock(stack -> stack.is(net.minecraft.world.item.Items.TWISTING_VINES), (client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.TWISTING_VINES)))
            .cleanup(MlgStrategies.breakBlock())
            .build()
    );

    private MlgState currentState = MlgState.IDLE;
    private FallPredictionResult currentPrediction = null;
    private MlgMethod activeMethod = null;
    private int idleGroundTicks = 0;

    private MlgStateMachine() {}

    public static MlgStateMachine getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        
        if (client.player.isDeadOrDying()) {
            if (currentState != MlgState.IDLE) {
                PiggyActionQueue.getInstance().clear("piggy-build");
                activeMethod = null;
                currentPrediction = null;
                transitionTo(MlgState.IDLE);
            }
            return;
        }

        Optional<FallPredictionResult> livePrediction = is.pig.minecraft.build.mlg.prediction.FallSimulator.simulate(client.player, client.level);

        if (currentState == MlgState.PREPARATION || currentState == MlgState.EXECUTION) {
            // Smooth horizontal adjustments (like WASD drift) organically change predictions!
            // We simply verify viability continues passing (e.g. didn't drift over a solid slab)
            // and smoothly stream the latest live prediction entirely omitting static trajectory cancellations.
            if (livePrediction.isEmpty() || !activeMethod.isViable(client, livePrediction.get())) {
                PiggyActionQueue.getInstance().clear("piggy-build");
                if (activeMethod != null) {
                    activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                }
                activeMethod = null;
                transitionTo(MlgState.FALLING);
            } else {
                currentPrediction = livePrediction.get();
            }
        }

        boolean stateChangedThisTick;
        int loopCount = 0;
        do {
            stateChangedThisTick = false;
            switch (currentState) {
                case IDLE -> {
                    if (livePrediction.isPresent() && livePrediction.get().isFatal()) {
                        currentPrediction = livePrediction.get();
                        stateChangedThisTick = transitionTo(MlgState.FALLING);
                    }
                }
                case FALLING -> {
                    if (livePrediction.isPresent()) {
                        currentPrediction = livePrediction.get();
                    } else {
                        activeMethod = null;
                        stateChangedThisTick = transitionTo(MlgState.IDLE);
                        break;
                    }

                    Optional<MlgMethod> bestMethod = MlgMethodSelector.selectBestMethod(client, currentPrediction, METHODS);
                    if (bestMethod.isPresent()) {
                        activeMethod = bestMethod.get();
                        LOGGER.debug("[MLG] Fall Ticks remaining: {}. Target prep: {}", currentPrediction.ticksToImpact(), activeMethod.getPreparationTickOffset(client, currentPrediction));
                        // Swap to item at the last minute to prevent user overrides and allow distance checks to breathe
                        if (currentPrediction.ticksToImpact() <= activeMethod.getPreparationTickOffset(client, currentPrediction)) {
                            stateChangedThisTick = transitionTo(MlgState.PREPARATION);
                        }
                    } else {
                        activeMethod = null;
                        if (currentPrediction.ticksToImpact() <= 5) { // Only spam warn at the end
                            LOGGER.warn("[MLG] No viable MLG methods found! Ticks: {}", currentPrediction.ticksToImpact());
                            stateChangedThisTick = transitionTo(MlgState.IDLE);
                        }
                    }
                }
                case PREPARATION -> {
                    activeMethod.queuePreparationActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                    stateChangedThisTick = transitionTo(MlgState.EXECUTION);
                }
                case EXECUTION -> {
                    if (activeMethod.canExecute(client, currentPrediction)) {
                        activeMethod.queueExecutionActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                        stateChangedThisTick = transitionTo(MlgState.RECOVERY);
                        // Hyper-flush the action queue to dispatch packets instantly to beat lethal impact processing loops!
                        PiggyActionQueue.getInstance().tick(client);
                    }
                }
                case RECOVERY -> {
                    // Holding organically. Wait for positive momentum bounces to settle fully natively!
                    if (client.player.onGround() && Math.abs(client.player.getDeltaMovement().y) < 0.001) {
                        idleGroundTicks++;
                    } else if ((client.player.isInWater() || client.player.onClimbable()) && Math.abs(client.player.getDeltaMovement().y) < 0.001) {
                        idleGroundTicks++;
                    } else {
                        idleGroundTicks = 0;
                    }
                    
                    int requiredTicks = activeMethod.requiresBounceSettlement() ? 15 : 0;
                    if (idleGroundTicks >= requiredTicks) {
                        // Prevent 0-tick RECOVERY escapes which infinitely loop! 
                        // Only gracefully exit into IDLE once the physics engine acknowledges the block and negates the fatal prediction, or confirms an upward bounce natively!
                        if (livePrediction.isEmpty() || !livePrediction.get().isFatal() || client.player.getDeltaMovement().y > 0 || idleGroundTicks > 10) {
                            activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                            activeMethod = null;
                            currentPrediction = null;
                            stateChangedThisTick = transitionTo(MlgState.IDLE);
                        }
                    } else if (client.player.getDeltaMovement().y < 0) {
                        // Monitor recursive slides preventing death drops organically during extended mid-air tracking natively
                        if (activeMethod.isPositionDependent() && livePrediction.isPresent() && livePrediction.get().isFatal()) {
                            // Instantaneous horizontal drift check bypassing server-client BlockState desynchronization delays natively
                            net.minecraft.core.BlockPos oldPos = currentPrediction.landingPos();
                            net.minecraft.core.BlockPos newPos = livePrediction.get().landingPos();
                            
                            // If they drift even 1 block horizontally from a 1x1 slime pad, it is completely fatal!
                            boolean severelyMissed = Math.abs(oldPos.getX() - newPos.getX()) > 0 || Math.abs(oldPos.getZ() - newPos.getZ()) > 0;

                            if (severelyMissed) {
                                PiggyActionQueue.getInstance().clear("piggy-build");
                                activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                                currentPrediction = livePrediction.get();
                                activeMethod = null; // Flush unsafe wrapper!
                                stateChangedThisTick = transitionTo(MlgState.FALLING);
                            }
                        }
                }
            }
            }
            loopCount++;
        } while (stateChangedThisTick && loopCount < 5);
    }


    private boolean transitionTo(MlgState newState) {
        if (this.currentState != newState) {
            LOGGER.info("[MLG] Transitioning to " + newState);
            this.currentState = newState;
            this.idleGroundTicks = 0;
            is.pig.minecraft.lib.action.PiggyActionQueue.getInstance().setSuppressAutoRefill(newState != MlgState.IDLE);
            return true;
        }
        return false;
    }
    
    public FallPredictionResult getCurrentPrediction() {
        return currentPrediction;
    }

    public MlgState getCurrentState() {
        return currentState;
    }

    public void forceFallState(Minecraft client) {
        if (client.player == null || client.level == null) return;
        
        Optional<FallPredictionResult> livePrediction = is.pig.minecraft.build.mlg.prediction.FallSimulator.simulate(client.player, client.level);
        if (livePrediction.isPresent() && livePrediction.get().isFatal()) {
            this.currentPrediction = livePrediction.get();
            transitionTo(MlgState.FALLING);
        } else {
            LOGGER.info("[MLG] Forced fall state but trajectory is non-fatal. Ignored.");
        }
    }
}
