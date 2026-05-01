package is.pig.minecraft.build.mlg.method.strategy;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgExecutionStrategy {
    void queueExecution(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);
}
