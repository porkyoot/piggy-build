package is.pig.minecraft.build.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessorMixin {
    // Allows reading the right-click delay
    @Accessor("rightClickDelay")
    int getRightClickDelay();

    // Allows modifying the right-click delay (used to force clicks)
    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);
}