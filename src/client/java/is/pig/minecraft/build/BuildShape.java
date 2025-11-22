package is.pig.minecraft.build;

import net.minecraft.resources.ResourceLocation;

public enum BuildShape {
    // On définit les noms de fichiers associés
    BLOCK("block"),
    LINE("line"),
    RING("ring"),
    SPHERE("sphere");

    private final ResourceLocation iconNormal;
    private final ResourceLocation iconSelected;

    BuildShape(String fileName) {
        // On pré-calcule les chemins pour ne pas le refaire à chaque image (Performance)
        this.iconNormal = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/gui/" + fileName + ".png");
        this.iconSelected = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/gui/" + fileName + "_selected.png");
    }

    public ResourceLocation getIcon(boolean isSelected) {
        return isSelected ? iconSelected : iconNormal;
    }
}