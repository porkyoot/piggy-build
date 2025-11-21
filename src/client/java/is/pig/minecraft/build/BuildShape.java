package is.pig.minecraft.build;

public enum BuildShape {
    BLOCK("Bloc Unique"), // Sera au centre
    LINE("Ligne"),        // Radial 1
    RING("Cercle"),       // Radial 2
    SPHERE("Sphère");     // Radial 3

    private final String displayName;

    BuildShape(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}