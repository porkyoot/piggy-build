package is.pig.minecraft.build.config;

import java.awt.Color;

import is.pig.minecraft.lib.config.PiggyClientConfig;
import is.pig.minecraft.lib.ui.AntiCheatFeedbackManager;
import is.pig.minecraft.lib.ui.BlockReason;

/**
 * Configuration data model for Piggy Build.
 * Holds the state of user settings.
 */
public class PiggyBuildConfig extends PiggyClientConfig {

    private static PiggyBuildConfig INSTANCE = new PiggyBuildConfig();

    // --- CONFIG FIELDS ---
    private Color highlightColor = new Color(0, 255, 230, 100);
    private Color placementOverlayColor = new Color(0, 255, 230, 255);

    // Fast placement settings
    private int fastPlaceDelayMs = 100;
    private boolean fastPlaceFeatureEnabled = false;

    // Flexible placement settings (master toggle for directional + diagonal)
    private boolean flexiblePlacementEnabled = true;

    // --- SINGLETON ACCESS ---

    public static PiggyBuildConfig getInstance() {
        return INSTANCE;
    }

    static void setInstance(PiggyBuildConfig instance) {
        INSTANCE = instance;
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

    public int getFastPlaceDelayMs() {
        return fastPlaceDelayMs;
    }

    public void setFastPlaceDelayMs(int delay) {
        this.fastPlaceDelayMs = delay;
    }

    public boolean isFastPlaceEnabled() {
        return fastPlaceFeatureEnabled;
    }

    public void setFastPlaceEnabled(boolean enabled) {
        // If attempting to enable, check server enforcement first
        if (enabled) {
            boolean serverForces = !this.serverAllowCheats
                    || (this.serverFeatures != null && this.serverFeatures.containsKey("fast_place")
                            && !this.serverFeatures.get("fast_place"));
            
            // Note: We do NOT block the setter for "No Cheating Mode" (client-side) here.
            // This allows the UI to update to "True" (preventing YACL mismatch crash),
            // but the feature will still be effectively disabled by 'isFeatureFastPlaceEnabled()'.
            
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
                            && !this.serverFeatures.get("flexible_placement"));
            
            // See note in setFastPlaceEnabled regarding No Cheating Mode.
            
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
        // Gray out if No Cheating Mode is active (Client side safety)
        if (isNoCheatingMode()) return false;
        
        // Gray out if Server forces it off
        if (!this.serverAllowCheats) return false;
        if (this.serverFeatures != null && this.serverFeatures.containsKey("fast_place") && !this.serverFeatures.get("fast_place")) return false;
        
        return true;
    }

    public boolean isFlexiblePlacementEditable() {
        if (isNoCheatingMode()) return false;
        
        if (!this.serverAllowCheats) return false;
        if (this.serverFeatures != null && this.serverFeatures.containsKey("flexible_placement") && !this.serverFeatures.get("flexible_placement")) return false;
        
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

    private int fastBreakDelayMs = 150;

    public int getFastBreakDelayMs() {
        return fastBreakDelayMs;
    }

    public void setFastBreakDelayMs(int fastBreakDelayMs) {
        this.fastBreakDelayMs = fastBreakDelayMs;
    }
}