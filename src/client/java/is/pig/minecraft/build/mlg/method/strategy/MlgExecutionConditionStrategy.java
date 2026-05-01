package is.pig.minecraft.build.mlg.method.strategy;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.FallPredictionResult;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgExecutionConditionStrategy {
    boolean canExecute(Minecraft client, FallPredictionResult prediction);

    default MlgExecutionConditionStrategy or(MlgExecutionConditionStrategy other) {
        return (client, prediction) -> this.canExecute(client, prediction) || other.canExecute(client, prediction);
    }
}
