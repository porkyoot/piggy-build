package is.pig.minecraft.build;

import net.minecraft.resources.ResourceLocation;

public enum BuildShape {
    BLOCK("block"),
    LINE("line"),
    RING("ring"),
    SPHERE("sphere");

    private final ResourceLocation icon;

    BuildShape(String fileName) {
        // Une seule texture par forme
        this.icon = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/gui/" + fileName + ".png");
    }

    public ResourceLocation getIcon() {
        return icon;
    }
}