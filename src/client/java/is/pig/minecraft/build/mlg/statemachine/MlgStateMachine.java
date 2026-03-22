package is.pig.minecraft.build.mlg.statemachine;

import is.pig.minecraft.build.mlg.method.AbstractMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethodSelector;
import is.pig.minecraft.build.mlg.method.SlimeBlockMlg;
import is.pig.minecraft.build.mlg.method.WaterBucketMlg;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Optional;

public class MlgStateMachine {

    private static final MlgStateMachine INSTANCE = new MlgStateMachine();
    private static final PiggyLog LOGGER = new PiggyLog("piggy-build", "MlgStateMachine");

    private static final List<AbstractMlgMethod> METHODS = List.of(new WaterBucketMlg(), new SlimeBlockMlg(), new is.pig.minecraft.build.mlg.method.BoatMlg(), new is.pig.minecraft.build.mlg.method.CobwebMlg());

    private MlgState currentState = MlgState.IDLE;
    private FallPredictionResult currentPrediction = null;
    private MlgMethod activeMethod = null;

    private MlgStateMachine() {}

    public static MlgStateMachine getInstance() {
        return INSTANCE;
    }

    public void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        
        Optional<FallPredictionResult> livePrediction = is.pig.minecraft.build.mlg.prediction.FallSimulator.simulate(client.player, client.level);

        if (currentState == MlgState.PREPARATION || currentState == MlgState.EXECUTION) {
            if (livePrediction.isEmpty() || isTrajectoryInterrupted(livePrediction.get(), currentPrediction)) {
                PiggyActionQueue.getInstance().clear("piggy-build");
                activeMethod = null;
                transitionTo(MlgState.FALLING);
            }
        }

        switch (currentState) {
            case IDLE:
                if (livePrediction.isPresent() && livePrediction.get().isFatal()) {
                    currentPrediction = livePrediction.get();
                    transitionTo(MlgState.FALLING);
                }
                break;
            case FALLING:
                Optional<MlgMethod> bestMethod = MlgMethodSelector.selectBestMethod(client, currentPrediction, METHODS);
                if (bestMethod.isPresent()) {
                    activeMethod = bestMethod.get();
                    transitionTo(MlgState.PREPARATION);
                } else {
                    activeMethod = null;
                    LOGGER.warn("[MLG] No viable MLG methods found!");
                    transitionTo(MlgState.IDLE);
                }
                break;
            case PREPARATION:
                activeMethod.queuePreparationActions(PiggyActionQueue.getInstance());
                transitionTo(MlgState.EXECUTION);
                break;
            case EXECUTION:
                currentPrediction = livePrediction.get();
                if (currentPrediction.ticksToImpact() <= activeMethod.getExecutionTickOffset()) {
                    activeMethod.queueExecutionActions(PiggyActionQueue.getInstance(), currentPrediction);
                    transitionTo(MlgState.RECOVERY);
                }
                break;
            case RECOVERY:
                if (client.player.onGround() || client.player.getDeltaMovement().y >= 0) {
                    transitionTo(MlgState.CLEANUP);
                }
                break;
            case CLEANUP:
                activeMethod.queueCleanupActions(PiggyActionQueue.getInstance());
                transitionTo(MlgState.IDLE);
                activeMethod = null;
                currentPrediction = null;
                break;
        }
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

    private void transitionTo(MlgState newState) {
        LOGGER.info("[MLG] Transitioning to " + newState);
        this.currentState = newState;
    }
    
    public FallPredictionResult getCurrentPrediction() {
        return currentPrediction;
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
