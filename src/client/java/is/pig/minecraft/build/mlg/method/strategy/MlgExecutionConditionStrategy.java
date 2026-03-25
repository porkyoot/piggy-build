package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.lib.util.telemetry.data.FallPredictionResult;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgExecutionConditionStrategy {
    boolean canExecute(Minecraft client, FallPredictionResult prediction);

    default MlgExecutionConditionStrategy or(MlgExecutionConditionStrategy other) {
        return (client, prediction) -> this.canExecute(client, prediction) || other.canExecute(client, prediction);
    }
}
