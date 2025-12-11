package is.pig.minecraft.build.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // Tool swap settings
    private boolean toolSwapEnabled = true;
    private List<Integer> swapHotbarSlots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
    private OrePreference orePreference = OrePreference.FORTUNE;

    public enum OrePreference {
        FORTUNE,
        SILK_TOUCH
    }

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
        if (enabled) {
            boolean serverForces = !this.serverAllowCheats
                    || (this.serverFeatures != null && this.serverFeatures.containsKey("fast_place")
                            && !this.serverFeatures.get("fast_place"));
            
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
            
            if (serverForces) {
                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("flexible_placement", BlockReason.SERVER_ENFORCEMENT);
                this.flexiblePlacementEnabled = false;
                return;
            }
        }
        this.flexiblePlacementEnabled = enabled;
    }

    // --- HELPERS FOR GUI AVAILABILITY ---

    /**
     * Determines if the global "No Cheating Mode" toggle can be edited.
     * Returns false if the server is enforcing anti-cheat (allowCheats = false).
     */
    public boolean isGlobalCheatsEditable() {
        return this.serverAllowCheats;
    }

    public boolean isFastPlaceEditable() {
        if (isNoCheatingMode()) return false;
        
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