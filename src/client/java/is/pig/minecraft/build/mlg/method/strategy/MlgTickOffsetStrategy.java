package is.pig.minecraft.build.mlg.method.strategy;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgTickOffsetStrategy {
    int getOffset(Minecraft client, FallPredictionResult prediction);
}
