package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
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
    void queuePreparationActions(PiggyActionQueue queue);

    /**
     * Queues actions to execute the MLG placing.
     */
    void queueExecutionActions(PiggyActionQueue queue, FallPredictionResult prediction);

    /**
     * Queues actions to clean up after the MLG (e.g., picking up water, breaking blocks).
     */
    void queueCleanupActions(PiggyActionQueue queue);

    /**
     * Determines how many ticks before impact the execution actions should be triggered.
     * Usually 1 or 2.
     */
    int getExecutionTickOffset();
}
