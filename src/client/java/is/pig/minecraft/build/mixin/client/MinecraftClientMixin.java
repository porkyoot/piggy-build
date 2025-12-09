package is.pig.minecraft.build.mixin.client;

import is.pig.minecraft.build.mvc.controller.DirectionalPlacementHandler;
import is.pig.minecraft.build.mvc.controller.InputController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    public HitResult hitResult;

    /**
     * Inject before startAttack to ensure tool swapping happens BEFORE the block is
     * hit/broken.
     */
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void piggyBuild$beforeAttack(CallbackInfoReturnable<Boolean> cir) {
        // This is the CRITICAL fix: Run tool swap logic immediately before the attack
        // happens.
        InputController.getToolSwapHandler().onTick((Minecraft) (Object) this);
    }

    /**
     * Redirect the useItemOn call to use our modified BlockHitResult.
     * This is more reliable than ModifyVariable as it directly controls what gets
     * passed.
     */
    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    @SuppressWarnings("ressource")
    private InteractionResult piggyBuild$redirectUseItemOn(
            MultiPlayerGameMode gameMode,
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult original) {

        // System.out.println("[MIXIN REDIRECT] Intercepted useItemOn call");
        // System.out.println("[MIXIN REDIRECT] Original face: " +
        // original.getDirection());

        Minecraft mc = (Minecraft) (Object) this;

        // Check if we should modify the hit result (directional OR diagonal mode)
        boolean directionalActive = InputController.directionalKey.isDown();
        boolean diagonalActive = InputController.diagonalKey.isDown();

        if ((directionalActive || diagonalActive) && this.hitResult instanceof BlockHitResult) {
            // Check No Cheating Mode
            Minecraft mc = (Minecraft) (Object) this;
            boolean isNoCheating = PiggyConfig.getInstance().isNoCheatingMode();
            boolean serverForces = !PiggyConfig.getInstance().serverAllowCheats && !mc.hasSingleplayerServer();

            if ((isNoCheating || serverForces) && gameMode.getPlayerMode() != GameType.CREATIVE) {
                if (mc.player != null) {
                    String message = serverForces
                            ? "Anti-Cheat Active: This server has forced anti-cheat ON."
                            : "Anti-Cheat Active: Disable 'No Cheating Mode' in settings to use.";

                    mc.player.displayClientMessage(
                            Component.literal(message)
                                    .withStyle(ChatFormatting.RED),
                            true // true = action bar
                    );
                }
                // Return original to behave like vanilla
                return gameMode.useItemOn(player, hand, original);
            }

            // String mode = directionalActive ? "DIRECTIONAL" : "DIAGONAL";
            // System.out.println("[MIXIN REDIRECT] " + mode + " mode active,
            // modifying...");

            DirectionalPlacementHandler handler = InputController.getDirectionalPlacementHandler();

            if (handler != null) {
                BlockHitResult modified = handler.modifyHitResult(mc, original);
                // System.out.println("[MIXIN REDIRECT] Modified face: " +
                // modified.getDirection());
                // System.out.println("[MIXIN REDIRECT] Modified pos: " +
                // modified.getBlockPos());

                // Call with the MODIFIED hit result
                return gameMode.useItemOn(player, hand, modified);
            }

        }

        // System.out.println("[MIXIN REDIRECT] Using original face");
        // No modification - use original
        return gameMode.useItemOn(player, hand, original);
    }
}