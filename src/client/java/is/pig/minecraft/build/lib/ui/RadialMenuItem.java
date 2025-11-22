package is.pig.minecraft.build.lib.ui;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

/**
 * Interface for any enum or object that wishes to be displayed in the GenericRadialMenu.
 */
public interface RadialMenuItem {
    ResourceLocation getIconLocation(boolean isSelected);
    Component getDisplayName();
}