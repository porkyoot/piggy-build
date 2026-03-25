package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.lib.util.telemetry.data.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

public interface MlgCleanupStrategy {
    void queueCleanup(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);
    boolean isFinished(Minecraft client, FallPredictionResult prediction);
}
