
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

    // Safety settings are inherited from PiggyClientConfig

    // Tool swap settings
    private boolean toolSwapEnabled = true;
    private List<Integer> swapHotbarSlots = new ArrayList<>()(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
    private OrePreference orePreference = OrePreference.FORTUNE;

    public enum OrePreference {
        FORTUNE,
        SILK_TOUCH
    }

    // Default lists
    private List<String> silkTouchBlocks = new ArrayList<>(Arrays.asList(
            "minecraft:glass", "minecraft:glass_pane", "minecraft:ice", "minecraft:packed_ice",
            "minecraft:blue_ice", "minecraft:ender_chest", "minecraft:turtle_egg", "minecraft:bee_nest",
            "minecraft:beehive", "minecraft:sculk", "minecraft:sculk_catalyst", "minecraft:sculk_sensor",
            "minecraft:sculk_shrieker", "*stained_glass*"));

    private List<String> fortuneBlocks = new ArrayList<>(Arrays.asList(
            "*_ore", "*ancient_debris*", "*amethyst_cluster*", "minecraft:clay",
            "minecraft:gravel", "minecraft:glowstone", "minecraft:melon", "minecraft:sea_lantern"));

    private List<String> shearsBlocks = new ArrayList<>(Arrays.asList(
            "minecraft:vine", "minecraft:dead_bush", "minecraft:short_grass", "minecraft:tall_grass",
            "minecraft:fern", "minecraft:large_fern", "*leaves*", "minecraft:cobweb",
            "minecraft:seagrass", "minecraft:hanging_roots", "minecraft:glow_lichen"));

    // --- SINGLETON ACCESS ---

    public static PiggyBuildConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Updates the singleton instance. Should only be called by ConfigPersistence.
     * 
     * @param instance The new instance loaded from disk.
     */
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
            if (serverForces) {
                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("fast_place", BlockReason.SERVER_ENFORCEMENT);
                return;
            }
        }
        this.fastPlaceFeatureEnabled = enabled;
    }

    public boolean isFlexiblePlacementEnabled() {
        return flexiblePlacementEnabled;
    }

    public void setFlexiblePlacementEnabled(boolean enabled) {
        // If attempting to enable, block if server disallows
        if (enabled) {
            boolean serverForces = !this.serverAllowCheats
                    || (this.serverFeatures != null && this.serverFeatures.containsKey("flexible_placement")
                            && !this.serverFeatures.get("flexible_placement"));
            if (serverForces) {
                AntiCheatFeedbackManager.getInstance().onFeatureBlocked("flexible_placement", BlockReason.SERVER_ENFORCEMENT);
                return;
            }
        }
        this.flexiblePlacementEnabled = enabled;
    }

    /**
     * Checks if fast place feature is actually enabled, considering server
     * overrides.
     */
    public boolean isFeatureFastPlaceEnabled() {
        return is.pig.minecraft.lib.features.CheatFeatureRegistry.isFeatureEnabled(
                "fast_place",
                serverAllowCheats,
                serverFeatures,
                isNoCheatingMode(),
                fastPlaceFeatureEnabled);
    }

    /**
     * Checks if flexible placement feature is actually enabled, considering server
     * overrides and the master toggle.
     */
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