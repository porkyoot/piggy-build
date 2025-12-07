package is.pig.minecraft.build.config;

import java.awt.Color;

/**
 * Configuration data model for Piggy Build.
 * Holds the state of user settings.
 */
public class PiggyConfig {

    private static PiggyConfig INSTANCE = new PiggyConfig();

    // --- CONFIG FIELDS ---
    private Color highlightColor = new Color(0, 255, 230, 100);
    private Color placementOverlayColor = new Color(0, 255, 230, 100);

    // Fast placement settings
    private int fastPlaceDelayMs = 100;
    private boolean fastPlaceEnabled = false;

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
}