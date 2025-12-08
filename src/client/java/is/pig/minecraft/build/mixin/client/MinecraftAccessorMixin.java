package is.pig.minecraft.build.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to get/set the rightClickDelay field in Minecraft.
 * This is used to control the delay between right-click actions.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessorMixin {

    @Accessor("rightClickDelay")
    int getRightClickDelay();

    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);

    @Accessor("missTime")
    void setMissTime(int delay);

    @org.spongepowered.asm.mixin.gen.Invoker("startAttack")
    boolean invokeStartAttack();
}