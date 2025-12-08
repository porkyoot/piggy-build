
package is.pig.minecraft.build.config;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration data model for Piggy Build.
 * Holds the state of user settings.
 */
public class PiggyConfig {

    private static PiggyConfig INSTANCE = new PiggyConfig();

    // --- CONFIG FIELDS ---
    private Color highlightColor = new Color(0, 255, 230, 100);
    private Color placementOverlayColor = new Color(0, 255, 230, 255);

    // Fast placement settings
    private int fastPlaceDelayMs = 100;
    private boolean fastPlaceEnabled = false;

    // Safety settings
    private boolean noCheatingMode = true;
    public transient boolean serverAllowCheats = true; // Runtime override from server

    // Tool swap settings
    private boolean toolSwapEnabled = true;
    private List<Integer> swapHotbarSlots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));
    private OrePreference orePreference = OrePreference.FORTUNE;

    public enum OrePreference {
        FORTUNE,
        SILK_TOUCH
    }

    // --- SINGLETON ACCESS ---

    public static PiggyConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Updates the singleton instance. Should only be called by ConfigPersistence.
     * 
     * @param instance The new instance loaded from disk.
     */
    static void setInstance(PiggyConfig instance) {
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
        return fastPlaceEnabled;
    }

    public void setFastPlaceEnabled(boolean enabled) {
        this.fastPlaceEnabled = enabled;
    }

    public boolean isNoCheatingMode() {
        return noCheatingMode;
    }

    public void setNoCheatingMode(boolean noCheatingMode) {
        this.noCheatingMode = noCheatingMode;
    }

    public boolean isToolSwapEnabled() {
        return toolSwapEnabled;
    }

    public void setToolSwapEnabled(boolean toolSwapEnabled) {
        this.toolSwapEnabled = toolSwapEnabled;
    }

    public List<Integer> getSwapHotbarSlots() {
        return swapHotbarSlots;
    }

    public void setSwapHotbarSlots(List<Integer> swapHotbarSlots) {
        this.swapHotbarSlots = swapHotbarSlots;
    }

    public OrePreference getOrePreference() {
        return orePreference;
    }

    public void setOrePreference(OrePreference orePreference) {
        this.orePreference = orePreference;
    }

    private int fastBreakDelayMs = 150;

    public int getFastBreakDelayMs() {
        return fastBreakDelayMs;
    }

    public void setFastBreakDelayMs(int fastBreakDelayMs) {
        this.fastBreakDelayMs = fastBreakDelayMs;
    }
}