package is.pig.minecraft.build.mvc.model;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.ui.RadialMenuItem;

public enum BuildShape implements RadialMenuItem {
    BLOCK("block", "Block"),
    LINE("line", "Line"),
    RING("ring", "Ring"),
    SPHERE("sphere", "Sphere");

    private final ResourceLocation icon;
    private final String displayName;

    BuildShape(String filename, String name) {
        this.icon = ResourceLocation.of("piggy-build", "textures/gui/" + filename + ".png");
        this.displayName = name;
    }

    @Override
    public ResourceLocation getIconLocation(boolean isSelected) {
        return icon;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}