package is.pig.minecraft.build.mlg.statemachine;

import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethodSelector;
import is.pig.minecraft.lib.util.telemetry.data.FallPredictionResult;
import is.pig.minecraft.build.mlg.method.impl.*;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;
import is.pig.minecraft.lib.util.telemetry.MetaActionSession;
import is.pig.minecraft.lib.util.telemetry.MetaActionSessionManager;
import is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Optional;

public class MlgStateMachine {

    private static final MlgStateMachine INSTANCE = new MlgStateMachine();
    private static final PiggyLog LOGGER = new PiggyLog("piggy-build", "MlgStateMachine");

    private static final List<MlgMethod> METHODS = List.of(
        ExistingMountableMlg.create(),
        MinecartMlg.create(),
        SaddleMlg.create(),
        ExistingMountableMlg.create(), // Intentionally duplicated in original code for weight? Leaving as is
        WaterBucketMlg.create(),
        SlimeBlockMlg.create(),
        CobwebMlg.create(),
        BoatMlg.create(),
        ChestBoatMlg.create(),
        ChorusFruitMlg.create(),
        TwistingVinesFertilizerMlg.create(),
        EnderPearlMlg.create(),
        HayBaleMlg.create(),
        BedMlg.create(),
        PowderSnowBucketMlg.create(),
        TwistingVinesMlg.create()
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
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> s.fail("Player died during MLG"));
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
                String reason = livePrediction.isEmpty() ? "Prediction vanished" : "Method no longer viable";
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> s.fail(reason));
                
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
                    final net.minecraft.world.entity.player.Player p = client.player;
                    MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                        s.logAction("Started MLG tracking", 
                                    is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter.formatPhysics(p),
                                    "Searching for methods");
                        s.info("Block beneath: " + is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter.formatBlock(
                            p.blockPosition().below(), client.level.getBlockState(p.blockPosition().below()), client.level));
                        s.info("Inventory Snapshot: " + is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter.formatContainer(p.getInventory()));
                    });

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
                            MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> s.fail("No viable methods found within impact window"));
                            stateChangedThisTick = transitionTo(MlgState.IDLE);
                        }
                    }
                }
                case PREPARATION -> {
                    final MlgMethod method = activeMethod;
                    final FallPredictionResult prediction = currentPrediction;
                    MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                        s.logAction("Impact imminent: " + prediction.ticksToImpact() + " ticks", 
                                    "Method: " + method.getName() + " | " + is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter.formatTrajectory(prediction, client.level),
                                    "Enqueuing Prep Actions");
                        s.info(is.pig.minecraft.lib.util.telemetry.formatter.PiggyTelemetryFormatter.formatNearbyEntities(prediction.landingPos(), 5.0, client.level));
                    });
                    activeMethod.queuePreparationActions(PiggyActionQueue.getInstance(), client, currentPrediction);
                    stateChangedThisTick = transitionTo(MlgState.EXECUTION);
                }
                case EXECUTION -> {
                    if (activeMethod.canExecute(client, currentPrediction)) {
                        final MlgMethod method = activeMethod;
                        final FallPredictionResult prediction = currentPrediction;
                        MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> 
                            s.logAction("Reached execution condition", 
                                        "Method: " + method.getName() + " | " + PiggyTelemetryFormatter.formatTrajectory(prediction, client.level),
                                        "Enqueuing Execution Actions"));
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
                                String how = String.format("Old:%s | New:%s", PiggyTelemetryFormatter.formatBlock(oldPos, null, client.level), PiggyTelemetryFormatter.formatBlock(newPos, null, client.level));
                                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> 
                                    s.logAction("Positional drift detected", how, "Aborting current method, returning to FALLING"));
                                
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
            String msg = "[MLG] Transitioning to " + newState;
            LOGGER.info(msg);
            MetaActionSessionManager.getInstance().log(Level.INFO, msg);

            if (newState == MlgState.FALLING && this.currentState == MlgState.IDLE) {
                MetaActionSession session = MetaActionSessionManager.getInstance().startSession("MLG");
                session.setPriority(true); // MLG is always high priority
                session.info("Started fatal fall tracking");
                
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    session.info("Initial Context: " + PiggyTelemetryFormatter.formatPlayer(client.player));
                }
                if (currentPrediction != null) {
                    session.info("Initial Prediction: " + PiggyTelemetryFormatter.formatTrajectory(currentPrediction, client.level));
                }
            } else if (newState == MlgState.PREPARATION) {
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                    s.info("Preparation with method: " + (activeMethod != null ? activeMethod.getName() : "none"));
                    if (currentPrediction != null) {
                        s.info("Target: " + PiggyTelemetryFormatter.formatBlock(currentPrediction.landingPos(), null, Minecraft.getInstance().level));
                    }
                });
            } else if (newState == MlgState.IDLE && this.currentState == MlgState.RECOVERY) {
                MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(s -> {
                    int ping = is.pig.minecraft.lib.util.perf.PerfMonitor.getInstance().getPing();
                    s.monitor(20 + ping / 50); // 1 second window + ping compensated
                });
            }

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
