package is.pig.minecraft.build.config;

import java.awt.Color;

import is.pig.minecraft.lib.config.PiggyClientConfig;
import is.pig.minecraft.lib.ui.AntiCheatFeedbackManager;
import is.pig.minecraft.lib.ui.BlockReason;

/**
 * Configuration data model for Piggy Build.
 * Holds the state of user settings.
 */
@SuppressWarnings("rawtypes")
public class PiggyBuildConfig extends PiggyClientConfig {

    private static PiggyBuildConfig INSTANCE = new PiggyBuildConfig();

    public PiggyBuildConfig() {
        super();
        PiggyClientConfig.setInstance(this);
        is.pig.minecraft.lib.config.PiggyConfigRegistry.getInstance().register(this);
    }

    @Override
    public void save() {
        ConfigPersistence.save();
    }

    // --- CONFIG FIELDS ---
    private Color highlightColor = new Color(0, 255, 230, 100);
    private Color placementOverlayColor = new Color(0, 255, 230, 255);
    private Color lightLevelOverlayColor = new Color(255, 0, 0, 255);
    private Color skyLightLevelOverlayColor = new Color(255, 165, 0, 255);

    private boolean fastPlaceFeatureEnabled = false;

    // Flexible placement settings (master toggle for directional + diagonal)
    private boolean flexiblePlacementEnabled = true;

    // --- SINGLETON ACCESS ---

    public static PiggyBuildConfig getInstance() {
        return INSTANCE;
    }

    static void setInstance(PiggyBuildConfig instance) {
        is.pig.minecraft.lib.config.PiggyConfigRegistry.getInstance().unregister(INSTANCE);
        INSTANCE = instance;
        is.pig.minecraft.lib.config.PiggyConfigRegistry.getInstance().register(INSTANCE);
    }

    // --- GETTERS / SETTERS ---

    public Color getHighlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
    }

    public Color getPlacementOverlayColor() {
        return placementOverlayColor;
    }

    public void setPlacementOverlayColor(Color placementOverlayColor) {
        this.placementOverlayColor = placementOverlayColor;
    }

    public Color getLightLevelOverlayColor() {
        return lightLevelOverlayColor;
    }

    public void setLightLevelOverlayColor(Color lightLevelOverlayColor) {
        this.lightLevelOverlayColor = lightLevelOverlayColor;
    }

    public Color getSkyLightLevelOverlayColor() {
        return skyLightLevelOverlayColor;
    }

    public void setSkyLightLevelOverlayColor(Color skyLightLevelOverlayColor) {
        this.skyLightLevelOverlayColor = skyLightLevelOverlayColor;
    }

    public boolean isFastPlaceEnabled() {
        return fastPlaceFeatureEnabled;
    }

    public void setFastPlaceEnabled(boolean enabled) {
        if (enabled) {
            boolean serverForces = !this.serverAllowCheats
                    // Note: Boolean cast via equals to avoid NPE/Object
                    || (this.serverFeatures != null && this.serverFeatures.containsKey("fast_place")
                            && !Boolean.TRUE.equals(this.serverFeatures.get("fast_place")));
            
            if (serverForces) {
                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("fast_place", BlockReason.SERVER_ENFORCEMENT);
                this.fastPlaceFeatureEnabled = false;
                return;
            }
        }
        this.fastPlaceFeatureEnabled = enabled;
    }

    public boolean isFlexiblePlacementEnabled() {
        return flexiblePlacementEnabled;
    }

    public void setFlexiblePlacementEnabled(boolean enabled) {
        if (enabled) {
            boolean serverForces = !this.serverAllowCheats
                    || (this.serverFeatures != null && this.serverFeatures.containsKey("flexible_placement")
                            && !Boolean.TRUE.equals(this.serverFeatures.get("flexible_placement")));
            
            if (serverForces) {
                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("flexible_placement", BlockReason.SERVER_ENFORCEMENT);
                this.flexiblePlacementEnabled = false;
                return;
            }
        }
        this.flexiblePlacementEnabled = enabled;
    }

    // --- HELPERS FOR GUI AVAILABILITY ---

    public boolean isFastPlaceEditable() {
        if (isNoCheatingMode()) return false;
        
        if (!this.serverAllowCheats) return false;
        // Fix object/boolean compilation error
        if (this.serverFeatures != null && this.serverFeatures.containsKey("fast_place") && !Boolean.TRUE.equals(this.serverFeatures.get("fast_place"))) return false;
        
        return true;
    }

    public boolean isFlexiblePlacementEditable() {
        if (isNoCheatingMode()) return false;
        
        if (!this.serverAllowCheats) return false;
        if (this.serverFeatures != null && this.serverFeatures.containsKey("flexible_placement") && !Boolean.TRUE.equals(this.serverFeatures.get("flexible_placement"))) return false;
        
        return true;
    }

    // --- LOGIC CHECKS ---

    public boolean isFeatureFastPlaceEnabled() {
        return is.pig.minecraft.lib.features.CheatFeatureRegistry.isFeatureEnabled(
                "fast_place",
                serverAllowCheats,
                serverFeatures,
                isNoCheatingMode(),
                fastPlaceFeatureEnabled);
    }

    public boolean isFeatureFlexiblePlacementEnabled() {
        return is.pig.minecraft.lib.features.CheatFeatureRegistry.isFeatureEnabled(
                "flexible_placement",
                serverAllowCheats,
                serverFeatures,
                isNoCheatingMode(),
                flexiblePlacementEnabled);
    }
    
    public boolean isFeatureShapeBuilderEnabled() {
        return is.pig.minecraft.lib.features.CheatFeatureRegistry.isFeatureEnabled(
                "shape_builder",
                serverAllowCheats,
                serverFeatures,
                isNoCheatingMode(),
                true); // Always enabled locally unless blocked by server or NoCheatingMode
    }
}