package is.pig.minecraft.build.mlg.statemachine;

import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.PhysicsAdapter;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethodSelector;
import is.pig.minecraft.api.*;
import is.pig.minecraft.build.mlg.method.impl.*;
import is.pig.minecraft.build.mlg.telemetry.MlgAttemptEvent;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.util.telemetry.MetaActionSession;
import is.pig.minecraft.lib.util.telemetry.MetaActionSessionManager;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;

import java.util.List;
import java.util.Optional;

public class MlgStateMachine {
    private static final MlgStateMachine INSTANCE = new MlgStateMachine();
    private static final List<MlgMethod> METHODS = List.of();

    private MlgState currentState = MlgState.IDLE;
    private FallPredictionResult currentPrediction = null;
    private MlgMethod activeMethod = null;
    private int idleGroundTicks = 0;
    private int idleCooldownTicks = 0;

    public boolean hasActiveMethod() {
        return activeMethod != null;
    }

    private MlgStateMachine() {}

    public static MlgStateMachine getInstance() {
        return INSTANCE;
    }

    public void tick(Object client) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        Object player = client;
        
        if (idleCooldownTicks > 0) {
            idleCooldownTicks--;
        }
        
        if (worldState.isDeadOrDying(player)) {
            if (currentState != MlgState.IDLE) {
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> s.fail("Player died during MLG"));
                PiggyActionQueue.getInstance().clear("piggy-build");
                activeMethod = null;
                currentPrediction = null;
                transitionTo(client, MlgState.IDLE);
            }
            return;
        }

        PhysicsAdapter physicsAdapter = PiggyServiceRegistry.getPhysicsAdapter();
        Optional<FallPredictionResult> livePrediction = physicsAdapter.simulateFall(player);

        if (currentState == MlgState.PREPARATION || currentState == MlgState.EXECUTION) {
            if (livePrediction.isEmpty() || !activeMethod.isViable(client, livePrediction.get())) {
                String reason = livePrediction.isEmpty() ? "Prediction vanished" : "Method no longer viable";
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> 
                    s.info("Switching method from " + activeMethod.getName() + " due to: " + reason));
                
                PiggyActionQueue.getInstance().clear("piggy-build");
                if (activeMethod != null) {
                    activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                }
                activeMethod = null;
                transitionTo(client, MlgState.FALLING);
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
                    if (livePrediction.isPresent() && livePrediction.get().isFatal() && idleCooldownTicks <= 0) {
                        currentPrediction = livePrediction.get();
                        stateChangedThisTick = transitionTo(client, MlgState.FALLING);
                    }
                }
                case FALLING -> {
                    MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                        s.logAction("Started MLG tracking", 
                                    "Player: " + worldState.getPlayerPos(player),
                                    "Searching for methods");
                    });

                    if (livePrediction.isPresent() && livePrediction.get().isFatal()) {
                        currentPrediction = livePrediction.get();
                    } else {
                        activeMethod = null;
                        stateChangedThisTick = transitionTo(client, MlgState.IDLE);
                        break;
                    }

                    Optional<MlgMethod> bestMethod = MlgMethodSelector.selectBestMethod(client, currentPrediction, METHODS);
                    if (bestMethod.isPresent()) {
                        activeMethod = bestMethod.get();
                        
                        final FallPredictionResult prediction = currentPrediction;
                        final MlgMethod method = activeMethod;
                        MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> 
                            s.logEvent(new MlgAttemptEvent(
                                prediction.fallDistance(),
                                method.getName(),
                                true,
                                prediction.isFatal(),
                                prediction.ticksToImpact()
                            ))
                        );
                        
                        int latencyTicks = Math.min(10, (int) Math.ceil(worldState.getPlayerLatency(client) / 50.0));
                        
                        if (currentPrediction.ticksToImpact() <= activeMethod.getPreparationTickOffset(client, currentPrediction) + latencyTicks) {
                            stateChangedThisTick = transitionTo(client, MlgState.PREPARATION);
                        }
                    } else {
                        activeMethod = null;
                        if (currentPrediction.ticksToImpact() <= 5) {
                            MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> s.fail("No viable survival method found"));
                            idleCooldownTicks = 10;
                            stateChangedThisTick = transitionTo(client, MlgState.IDLE);
                        }
                    }
                }
                case PREPARATION -> {
                    final MlgMethod method = activeMethod;
                    final FallPredictionResult prediction = currentPrediction;
                    MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                        s.logAction("Impact imminent: " + prediction.ticksToImpact() + " ticks", 
                                    "Method: " + method.getName(),
                                    "Enqueuing Prep Actions");
                    });
                    activeMethod.queuePreparationActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                    stateChangedThisTick = transitionTo(client, MlgState.EXECUTION);
                }
                case EXECUTION -> {
                    if (activeMethod.canExecute(client, currentPrediction)) {
                        final MlgMethod method = activeMethod;
                        MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> 
                            s.logAction("Reached execution condition", 
                                        "Method: " + method.getName(),
                                        "Enqueuing Execution Actions"));
                        activeMethod.queueExecutionActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                        stateChangedThisTick = transitionTo(client, MlgState.RECOVERY);
                        PiggyActionQueue.getInstance().tick(client);
                    }
                }
                case RECOVERY -> {
                    Vec3 velocity = worldState.getPlayerVelocity(player);
                    if ((worldState.isOnGround(player) || worldState.isInWater(player)) && Math.abs(velocity.y()) < 0.001) {
                        idleGroundTicks++;
                    } else {
                        idleGroundTicks = 0;
                    }
                    
                    int requiredTicks = activeMethod.requiresBounceSettlement() ? 15 : 0;
                    if (idleGroundTicks >= requiredTicks) {
                        if (livePrediction.isEmpty() || !livePrediction.get().isFatal() || velocity.y() > 0 || idleGroundTicks > 10) {
                            activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                            activeMethod = null;
                            currentPrediction = null;
                            stateChangedThisTick = transitionTo(client, MlgState.IDLE);
                        }
                    } else if (velocity.y() < 0) {
                        if (activeMethod.isPositionDependent() && livePrediction.isPresent() && livePrediction.get().isFatal()) {
                            BlockPos oldPos = currentPrediction.landingPos();
                            BlockPos newPos = livePrediction.get().landingPos();
                            boolean severelyMissed = Math.abs(oldPos.x() - newPos.x()) > 0 || Math.abs(oldPos.z() - newPos.z()) > 0;

                            if (severelyMissed) {
                                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> 
                                    s.info("Positional drift detected. Aborting current method, returning to FALLING"));
                                
                                PiggyActionQueue.getInstance().clear("piggy-build");
                                activeMethod.queueCleanupActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                                currentPrediction = livePrediction.get();
                                activeMethod = null;
                                stateChangedThisTick = transitionTo(client, MlgState.FALLING);
                            }
                        }
                    }
                }
            }
            loopCount++;
        } while (stateChangedThisTick && loopCount < 5);
    }

    private boolean transitionTo(Object client, MlgState newState) {
        if (this.currentState != newState) {
            WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
            if (newState == MlgState.FALLING && this.currentState == MlgState.IDLE) {
                MetaActionSession session = MetaActionSessionManager.getInstance().startSession("MLG");
                session.setPriority(true);
                session.info("Started fatal fall tracking");
                session.info("Initial Context: " + worldState.getPlayerPos(client));
            } else if (newState == MlgState.PREPARATION) {
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                    s.info("Preparation with method: " + (activeMethod != null ? activeMethod.getName() : "none"));
                });
            } else if (newState == MlgState.IDLE && this.currentState != MlgState.IDLE) {
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                    int ping = worldState.getPlayerLatency(client);
                    s.monitor(20 + ping / 50);
                });
            }

            this.currentState = newState;
            this.idleGroundTicks = 0;
            PiggyActionQueue.getInstance().setSuppressAutoRefill(newState != MlgState.IDLE);
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

    public void forceFallState(Object client) {
        PhysicsAdapter physicsAdapter = PiggyServiceRegistry.getPhysicsAdapter();
        Optional<FallPredictionResult> livePrediction = physicsAdapter.simulateFall(client);
        if (livePrediction.isPresent() && livePrediction.get().isFatal()) {
            this.currentPrediction = livePrediction.get();
            transitionTo(client, MlgState.FALLING);
        }
    }
}
