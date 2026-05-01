package is.pig.minecraft.build.mlg.method.strategy;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.FallPredictionResult;
import net.minecraft.client.Minecraft;

@FunctionalInterface
public interface MlgViabilityStrategy {
    boolean isViable(Minecraft client, FallPredictionResult prediction);
    
    default MlgViabilityStrategy and(MlgViabilityStrategy other) {
        return (client, prediction) -> this.isViable(client, prediction) && other.isViable(client, prediction);
    }
}
