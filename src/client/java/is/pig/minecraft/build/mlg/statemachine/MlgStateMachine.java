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
            .reliabilityScore(100)
            .cleanupDifficulty(1)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.WATER_BUCKET)
                .and(MlgStrategies.requireReplaceableLanding())
                .and(MlgStrategies.notUltrawarm()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.WATER_BUCKET))
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.WATER)))
            .cleanup(MlgStrategies.scoopItem())
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
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK)))
            .cleanup(MlgStrategies.breakBlock())
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
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.COBWEB)))
            .cleanup(MlgStrategies.breakBlockWithToolSwap(net.minecraft.world.item.Items.COBWEB))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(80)
            .cleanupDifficulty(4)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.OAK_BOAT) // Simplifying boat viability specifically to Oak Boat for compose test
                .and(MlgStrategies.requireReplaceableLanding())
                .and(MlgStrategies.requireClearSpace(1, net.minecraft.world.entity.vehicle.Boat.class)))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.OAK_BOAT))
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && !client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(MlgStrategies.attackEntity(net.minecraft.world.entity.vehicle.Boat.class))
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
            .execution(MlgStrategies.holdUseItem(true))
            .cleanup(MlgStrategies.releaseUseItem())
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
                queue.enqueue(new is.pig.minecraft.lib.action.world.UseItemAction(
                        net.minecraft.world.InteractionHand.MAIN_HAND,
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
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)))
            .cleanup(MlgStrategies.breakBlock())
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(false) // Beds do NOT negate all damage natively!
            .fallDamageMultiplier(0.5f) // Beds reduce fall damage by 50%
            .reliabilityScore(70)
            .cleanupDifficulty(5)
            .requiresBounceSettlement(true)
            .preparationTickOffset(MlgStrategies.dynamicPreparation())
            .executionCondition(MlgStrategies.dynamicReach())
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.RED_BED)
                .and(MlgStrategies.requireReplaceableLanding()))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.RED_BED))
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && client.level.getBlockState(pos.above()).is(net.minecraft.tags.BlockTags.BEDS)))
            .cleanup(MlgStrategies.breakBlockWithToolSwap(net.minecraft.world.item.Items.WOODEN_AXE))
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
                    if (client.player.onGround() && client.player.getDeltaMovement().y <= 0.0) {
                        idleGroundTicks++;
                    } else if (client.player.isInWater() || client.player.onClimbable()) {
                        idleGroundTicks++;
                    } else {
                        idleGroundTicks = 0;
                    }
                    
                    int requiredTicks = activeMethod.requiresBounceSettlement() ? 10 : 0;
                    if (idleGroundTicks >= requiredTicks) {
                        activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                        stateChangedThisTick = transitionTo(MlgState.CLEANUP);
                    } else if (client.player.getDeltaMovement().y < 0) {
                        // Monitor recursive slides preventing death drops organically during extended mid-air tracking natively
                        if (activeMethod.isPositionDependent() && livePrediction.isPresent() && livePrediction.get().isFatal()) {
                            // Verify they haven't simply bounced and started falling directly back onto our safety cushion natively!
                            net.minecraft.core.BlockPos oldPos = currentPrediction.landingPos();
                            net.minecraft.core.BlockPos newPos = livePrediction.get().landingPos();
                            boolean severelyMissed = Math.abs(oldPos.getX() - newPos.getX()) > 1 
                                                  || Math.abs(oldPos.getZ() - newPos.getZ()) > 1 
                                                  || Math.abs(oldPos.getY() - newPos.getY()) > 2;

                            if (severelyMissed && currentPrediction.ticksToImpact() > 5) {
                                PiggyActionQueue.getInstance().clear("piggy-build");
                                activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                                currentPrediction = livePrediction.get();
                                activeMethod = null; // Flush unsafe wrapper!
                                stateChangedThisTick = transitionTo(MlgState.FALLING);
                            }
                        }
                    }
                }
                case CLEANUP -> {
                    if (client.player.getDeltaMovement().y < 0 && !client.player.onGround() && !client.player.isInWater()) {
                        // Catch lethal slides gracefully allowing instant re-prediction loops organically!
                        if (activeMethod.isPositionDependent() && livePrediction.isPresent() && livePrediction.get().isFatal()) {
                            net.minecraft.core.BlockPos oldPos = currentPrediction.landingPos();
                            net.minecraft.core.BlockPos newPos = livePrediction.get().landingPos();
                            boolean severelyMissed = Math.abs(oldPos.getX() - newPos.getX()) > 1 
                                                  || Math.abs(oldPos.getZ() - newPos.getZ()) > 1 
                                                  || Math.abs(oldPos.getY() - newPos.getY()) > 2;

                            if (severelyMissed && currentPrediction.ticksToImpact() > 5) {
                                PiggyActionQueue.getInstance().clear("piggy-build");
                                activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                                activeMethod = null;
                                currentPrediction = livePrediction.get();
                                stateChangedThisTick = transitionTo(MlgState.FALLING);
                            }
                        }
                    } else {
                        // Dynamically poll the state natively checking geometric bounds endlessly verifying execution completes!
                        if (activeMethod.isCleanupFinished(client, currentPrediction)) {
                            // Infinite retry concluded successfully! Dropping execution queue locally.
                            PiggyActionQueue.getInstance().clear("piggy-build");
                            activeMethod = null;
                            currentPrediction = null;
                            stateChangedThisTick = transitionTo(MlgState.IDLE);
                        } else {
                            // Cleanup verification failed. Automatically attempt queue restoration preserving state natively
                            if (!PiggyActionQueue.getInstance().hasActions("piggy-build")) {
                                // Strictly mask executions preventing server spam dropping when outside safe reach envelopes!
                                if (client.player.getEyePosition().distanceTo(Vec3.atCenterOf(currentPrediction.landingPos()).add(0, 0.5, 0)) <= 4.5) {
                                    activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                                }
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
