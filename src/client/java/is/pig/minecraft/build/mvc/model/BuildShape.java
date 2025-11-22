package is.pig.minecraft.build.mvc.model;

import is.pig.minecraft.build.lib.ui.RadialMenuItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum BuildShape implements RadialMenuItem {
    BLOCK("block", "Block"),
    LINE("line", "Line"),
    RING("ring", "Ring"),
    SPHERE("sphere", "Sphere");

    private final ResourceLocation icon;
    private final Component displayName;

    BuildShape(String filename, String name) {
        this.icon = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/gui/" + filename + ".png");
        this.displayName = Component.literal(name);
    }

    @Override
    public ResourceLocation getIconLocation(boolean isSelected) {
        return icon;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }
}