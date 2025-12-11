package is.pig.minecraft.build.mixin.client;

import is.pig.minecraft.build.mvc.controller.DirectionalPlacementHandler;
import is.pig.minecraft.build.mvc.controller.InputController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    public HitResult hitResult;

    @Redirect(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    @SuppressWarnings("ressource")
    private InteractionResult piggyBuild$redirectUseItemOn(
            MultiPlayerGameMode gameMode,
            LocalPlayer player,
            InteractionHand hand,
            BlockHitResult original) {

        Minecraft mc = (Minecraft) (Object) this;

        boolean directionalActive = InputController.directionalKey.isDown();
        boolean diagonalActive = InputController.diagonalKey.isDown();

        if ((directionalActive || diagonalActive) && this.hitResult instanceof BlockHitResult) {
            PiggyBuildConfig config = PiggyBuildConfig.getInstance();
            boolean isEnabled = config.isFeatureFlexiblePlacementEnabled();

            // If feature is disabled, simply fall back to vanilla behavior.
            // Feedback is handled by DirectionalPlacementHandler or Config Setters.
            if (!isEnabled && gameMode.getPlayerMode() != GameType.CREATIVE) {
                return gameMode.useItemOn(player, hand, original);
            }

            DirectionalPlacementHandler handler = InputController.getDirectionalPlacementHandler();
            if (handler != null) {
                BlockHitResult modified = handler.modifyHitResult(mc, original);
                return gameMode.useItemOn(player, hand, modified);
            }
        }

        return gameMode.useItemOn(player, hand, original);
    }
}