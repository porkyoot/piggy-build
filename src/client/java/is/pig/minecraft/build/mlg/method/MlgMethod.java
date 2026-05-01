package is.pig.minecraft.build.mlg.method;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.api.FallPredictionResult;

/**
 * Interface representing a specific MLG method.
 */
public interface MlgMethod {

    /**
     * @return The display name of the MLG method.
     */
    default String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Determines if this MLG method is viable based on the prediction.
     */
    boolean isViable(Object client, FallPredictionResult prediction);

    /**
     * Queues actions to prepare for the MLG (e.g., swapping tools, rotating camera).
     */
    void queuePreparationActions(PiggyActionQueue queue, Object client, FallPredictionResult prediction);

    /**
     * Determines how many ticks before impact the PREPARATION actions should be triggered dynamically.
     */
    int getPreparationTickOffset(Object client, FallPredictionResult prediction);

    /**
     * Queues actions to execute the MLG placing.
     */
    void queueExecutionActions(PiggyActionQueue queue, Object client, FallPredictionResult prediction);

    /**
     * Queues actions to clean up after the MLG (e.g., picking up water, breaking blocks).
     */
    void queueCleanupActions(PiggyActionQueue queue, Object client, FallPredictionResult prediction);

    /**
     * Dynamically continuously queries if the requested cleanup actions have fully resolved geometry in the world natively.
     */
    boolean isCleanupFinished(Object client, FallPredictionResult prediction);

    /**
     * Dynamically determines whether the execution actions should be triggered based on player reach, velocity, or ticks to impact.
     */
    boolean canExecute(Object client, FallPredictionResult prediction);

    /**
     * Determines the base self-inflicted recoil damage of this method (e.g. 5.0f for pearls) mathematically prior to armor evaluations
     */
    float getSelfDamage();

    /**
     * The percentage of raw fall damage preserved if this method executes (e.g. 0.2f for Hay Bales, 0.5f for Beds).
     * Only evaluated strictly if negatesAllDamage is false.
     */
    float getFallDamageMultiplier();

    /**
     * Determines whether the State Machine should dynamically hold in RECOVERY for 10 ticks measuring kinetic cessation strictly bypassing Slime/Bed ricochets safely.
     */
    boolean requiresBounceSettlement();

    /**
     * Determines whether the method critically relies on the player landing precisely on the original predicted trajectory.
     * Set to false for teleportations or entity mounts.
     */
    boolean isPositionDependent();

    /**
     * Determines if this method unconditionally negates physical fall damage organically.
     */
    boolean negatesAllDamage();

    /**
     * Metric evaluating physics reliability globally [0-100].
     */
    int getReliabilityScore(Object client, FallPredictionResult prediction);

    /**
     * Difficulty scale for automated cleanup organically.
     */
    int getCleanupDifficulty();

    /**
     * Determines the total item cost/consumption of this method (0 = perfectly reusable, 1+ = consumed)
     */
    int getItemConsumptionCost();
}
