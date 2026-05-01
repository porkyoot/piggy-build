package is.pig.minecraft.build.mlg.method.strategy;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

public interface MlgCleanupStrategy {
    void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);
    boolean isFinished(Minecraft client, FallPredictionResult prediction);
}
