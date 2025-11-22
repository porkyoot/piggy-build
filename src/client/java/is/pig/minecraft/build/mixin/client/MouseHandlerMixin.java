package is.pig.minecraft.build.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import is.pig.minecraft.build.lib.event.MouseScrollCallback;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // On déclenche notre événement
        // Si vertical != 0 (il y a un scroll)
        if (vertical != 0) {
            boolean cancelled = MouseScrollCallback.EVENT.invoker().onScroll(vertical);
            if (cancelled) {
                ci.cancel(); // On empêche Minecraft de changer l'item dans la main
            }
        }
    }
}