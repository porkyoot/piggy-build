package is.pig.minecraft.build.mlg.statemachine;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethodSelector;
import is.pig.minecraft.build.mlg.method.strategy.MlgStrategies;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

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
            .executionCondition(MlgStrategies.withinTicks(5))
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
            .executionCondition(MlgStrategies.withinTicks(5))
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
            .executionCondition(MlgStrategies.withinTicks(5))
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
            .executionCondition(MlgStrategies.withinTicks(5))
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.OAK_BOAT) // Simplifying boat viability specifically to Oak Boat for compose test
                .and(MlgStrategies.requireReplaceableLanding())
                .and(MlgStrategies.requireClearSpace(1, net.minecraft.world.entity.vehicle.Boat.class)))
            .preparation(MlgStrategies.swapToItemAndLookDown(net.minecraft.world.item.Items.OAK_BOAT))
            .execution(MlgStrategies.interactBlock((client, pos) -> client.level != null && !client.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.Boat.class, new net.minecraft.world.phys.AABB(pos).inflate(2), e -> true).isEmpty()))
            .cleanup(MlgStrategies.attackEntity(net.minecraft.world.entity.vehicle.Boat.class))
            .build(),

        ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(99)
            .cleanupDifficulty(1)
            .preparationTickOffset(1000) // Allows this to instantly prepare out of logic loop
            .executionCondition(MlgStrategies.withinTicks(1000)) // Instantly start executing
            .viability(MlgStrategies.requireItem(net.minecraft.world.item.Items.CHORUS_FRUIT)
                .and(MlgStrategies.requireTicksToImpactGreaterThan(35))) // It physically takes 32 ticks to eat food
            .preparation(MlgStrategies.swapToItem(net.minecraft.world.item.Items.CHORUS_FRUIT))
            .execution(MlgStrategies.holdUseItem(true))
            .cleanup(MlgStrategies.releaseUseItem())
            .build()
    );

    private MlgState currentState = MlgState.IDLE;
    private FallPredictionResult currentPrediction = null;
    private MlgMethod activeMethod = null;

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
            // Note: During Chorus MLG Execution the player eats, but they are still technically executing and falling! 
            // The simulation only gets interrupted if their fall vector shifts significantly (like teleporting to the ground)
            // When teleporting happens, isTrajectoryInterrupted triggers which drops them into FALLING.
            // Wait, if they teleport safely to the ground, we WANT them to trigger cleanup!
            if (livePrediction.isEmpty() || isTrajectoryInterrupted(livePrediction.get(), currentPrediction)) {
                PiggyActionQueue.getInstance().clear("piggy-build");
                if (activeMethod != null) {
                    activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                }
                activeMethod = null;
                transitionTo(MlgState.FALLING);
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
                        LOGGER.debug("[MLG] Fall Ticks remaining: {}. Target prep: {}", currentPrediction.ticksToImpact(), activeMethod.getPreparationTickOffset());
                        // Swap to item at the last minute to prevent user overrides and allow distance checks to breathe
                        if (currentPrediction.ticksToImpact() <= activeMethod.getPreparationTickOffset()) {
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
                    currentPrediction = livePrediction.get();
                    if (activeMethod.canExecute(client, currentPrediction)) {
                        activeMethod.queueExecutionActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                        stateChangedThisTick = transitionTo(MlgState.RECOVERY);
                        // Hyper-flush the action queue to dispatch packets instantly to beat lethal impact processing loops!
                        PiggyActionQueue.getInstance().tick(client);
                    }
                }
                case RECOVERY -> {
                    boolean isSafeState = client.player.onGround() || 
                                          client.player.getDeltaMovement().y >= 0 ||
                                          client.player.isInWater() || 
                                          client.player.isInLava() ||
                                          client.level.getBlockState(client.player.blockPosition()).is(net.minecraft.world.level.block.Blocks.COBWEB) ||
                                          client.level.getBlockState(client.player.blockPosition()).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW) ||
                                          client.player.isPassenger();

                    if (isSafeState) {
                        activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                        stateChangedThisTick = transitionTo(MlgState.CLEANUP);
                    }
                }
                case CLEANUP -> {
                    if (client.player.getDeltaMovement().y < 0 && !client.player.onGround()) {
                        if (livePrediction.isPresent()) {
                            currentPrediction = livePrediction.get();
                            stateChangedThisTick = transitionTo(MlgState.PREPARATION);
                        }
                    } else if (client.player.onGround() && !PiggyActionQueue.getInstance().hasActions("piggy-build")) {
                        stateChangedThisTick = transitionTo(MlgState.IDLE);
                        activeMethod = null;
                        currentPrediction = null;
                    }
                }
            }
            loopCount++;
        } while (stateChangedThisTick && loopCount < 5);
    }

    private boolean isTrajectoryInterrupted(FallPredictionResult live, FallPredictionResult stored) {
        if (stored == null) return false;
        double dx = live.landingPos().getX() - stored.landingPos().getX();
        double dz = live.landingPos().getZ() - stored.landingPos().getZ();
        if (Math.sqrt(dx * dx + dz * dz) > 1.5) {
            return true;
        }
        if (Math.abs(live.ticksToImpact() - stored.ticksToImpact()) > 10) {
            return true;
        }
        return false;
    }

    private boolean transitionTo(MlgState newState) {
        if (this.currentState != newState) {
            LOGGER.info("[MLG] Transitioning to " + newState);
            this.currentState = newState;
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
