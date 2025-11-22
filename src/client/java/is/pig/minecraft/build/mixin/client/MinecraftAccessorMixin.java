package is.pig.minecraft.build.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessorMixin {
    // Permet de lire le délai
    @Accessor("rightClickDelay")
    int getRightClickDelay();

    // Permet de modifier le délai (C'est ça qu'on veut !)
    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);
}