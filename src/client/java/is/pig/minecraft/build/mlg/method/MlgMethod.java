package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import net.minecraft.client.Minecraft;

/**
 * Interface representing a specific MLG method.
 */
public interface MlgMethod {

    /**
     * Determines if this MLG method is viable based on the prediction.
     */
    boolean isViable(Minecraft client, FallPredictionResult prediction);

    /**
     * Queues actions to prepare for the MLG (e.g., swapping tools, rotating camera).
     */
    void queuePreparationActions(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);

    /**
     * Determines how many ticks before impact the PREPARATION actions should be triggered.
     * Defaults to 5; higher values trigger preparation immediately if required (e.g., Chorus Fruit consumption).
     */
    int getPreparationTickOffset();

    /**
     * Queues actions to execute the MLG placing.
     */
    void queueExecutionActions(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);

    /**
     * Queues actions to clean up after the MLG (e.g., picking up water, breaking blocks).
     */
    void queueCleanupActions(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);

    /**
     * Dynamically determines whether the execution actions should be triggered based on player reach, velocity, or ticks to impact.
     */
    boolean canExecute(Minecraft client, FallPredictionResult prediction);
}
