package is.pig.minecraft.build.mixin.client;

import is.pig.minecraft.build.mvc.controller.DirectionalPlacementHandler;
import is.pig.minecraft.build.mvc.controller.InputController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    public HitResult hitResult;

    /**
     * Redirect the useItemOn call to use our modified BlockHitResult.
     * This is more reliable than ModifyVariable as it directly controls what gets
     * passed.
     */
    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult piggyBuild$redirectUseItemOn(
            MultiPlayerGameMode gameMode,
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult original) {

        System.out.println("[MIXIN REDIRECT] Intercepted useItemOn call");
        System.out.println("[MIXIN REDIRECT] Original face: " + original.getDirection());

        // Check if we should modify the hit result (directional OR diagonal mode)
        boolean directionalActive = InputController.directionalKey.isDown();
        boolean diagonalActive = InputController.diagonalKey.isDown();

        if ((directionalActive || diagonalActive) && this.hitResult instanceof BlockHitResult) {
            String mode = directionalActive ? "DIRECTIONAL" : "DIAGONAL";
            System.out.println("[MIXIN REDIRECT] " + mode + " mode active, modifying...");

            Minecraft mc = (Minecraft) (Object) this;
            DirectionalPlacementHandler handler = InputController.getDirectionalPlacementHandler();

            if (handler != null) {
                BlockHitResult modified = handler.modifyHitResult(mc, original);
                System.out.println("[MIXIN REDIRECT] Modified face: " + modified.getDirection());
                System.out.println("[MIXIN REDIRECT] Modified pos: " + modified.getBlockPos());

                // Call with the MODIFIED hit result
                return gameMode.useItemOn(player, hand, modified);
            }
        }

        System.out.println("[MIXIN REDIRECT] Using original face");
        // No modification - use original
        return gameMode.useItemOn(player, hand, original);
    }
}