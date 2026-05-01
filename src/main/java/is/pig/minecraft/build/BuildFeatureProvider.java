package is.pig.minecraft.build;

import is.pig.minecraft.api.spi.FeatureProvider;
import is.pig.minecraft.api.spi.PiggyFeature;

import java.util.List;

public class BuildFeatureProvider implements FeatureProvider {
    @Override
    public List<PiggyFeature> getFeatures() {
        return List.of(
            new PiggyFeature("piggy-build:auto-mlg", "Auto-MLG", true, "Automatically survives fatal falls", null),
            new PiggyFeature("piggy-build:fast-place", "Fast Place", false, "Speeds up block placement", null),
            new PiggyFeature("piggy-build:flexible-placement", "Flexible Placement", true, "Directional and diagonal placement", null),
            new PiggyFeature("piggy-build:auto-parkour", "Auto Parkour", false, "Automatically jumps over gaps", null)
        );
    }
}
