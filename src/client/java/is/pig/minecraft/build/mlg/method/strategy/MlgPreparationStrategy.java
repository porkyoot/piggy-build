package is.pig.minecraft.build.mlg.method.strategy;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.FallPredictionResult;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgPreparationStrategy {
    void queuePreparation(PiggyActionQueue queue, Minecraft client, FallPredictionResult prediction);
    
    default MlgPreparationStrategy andThen(MlgPreparationStrategy after) {
        return (queue, client, prediction) -> {
            this.queuePreparation(queue, client, prediction);
            after.queuePreparation(queue, client, prediction);
        };
    }
}
