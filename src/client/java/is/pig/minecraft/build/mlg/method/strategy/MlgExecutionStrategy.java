package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgExecutionStrategy {
    void queueExecution(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);
}
