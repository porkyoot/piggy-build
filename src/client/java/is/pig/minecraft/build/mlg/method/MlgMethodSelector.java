package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import net.minecraft.client.Minecraft;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MlgMethodSelector {

    public static Optional<MlgMethod> selectBestMethod(Minecraft client, FallPredictionResult prediction, List<MlgMethod> availableMethods) {
        return availableMethods.stream()
                .filter(method -> method.isViable(client, prediction))
                .max(Comparator.comparing((MlgMethod m) -> m.negatesAllDamage() ? 1 : 0)
                        .thenComparing(MlgMethod::getReliabilityScore)
                        .thenComparing(m -> -m.getCleanupDifficulty()))
                .map(m -> m);
    }
}
