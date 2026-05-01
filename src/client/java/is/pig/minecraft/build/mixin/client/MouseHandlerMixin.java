package is.pig.minecraft.build.mixin.client;
import is.pig.minecraft.api.*;

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
        // Fire our event
        // If vertical != 0 (there is a scroll)
        if (vertical != 0) {
            boolean cancelled = MouseScrollCallback.EVENT.invoker().onScroll(vertical);
            if (cancelled) {
                ci.cancel(); // Prevent Minecraft from changing the held item
            }
        }
    }

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(double timeOrDelta, CallbackInfo ci) {
        is.pig.minecraft.build.mlg.statemachine.MlgState state = is.pig.minecraft.build.mlg.statemachine.MlgStateMachine.getInstance().getCurrentState();
        if (state == is.pig.minecraft.build.mlg.statemachine.MlgState.EXECUTION || state == is.pig.minecraft.build.mlg.statemachine.MlgState.PREPARATION) {
            ci.cancel();
        }
    }
}