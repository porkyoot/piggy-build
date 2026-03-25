package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.lib.util.telemetry.data.FallPredictionResult;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgTickOffsetStrategy {
    int getOffset(Minecraft client, FallPredictionResult prediction);
}
