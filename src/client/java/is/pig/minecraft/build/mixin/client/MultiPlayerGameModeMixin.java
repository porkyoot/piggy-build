package is.pig.minecraft.build.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.mvc.controller.FlexiblePlacementHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    
    @ModifyVariable(
        method = "useItemOn",
        at = @At("HEAD"),
        argsOnly = true
    )
    private BlockHitResult modifyHitResult(BlockHitResult original) {
        BlockHitResult modified = FlexiblePlacementHandler.getModifiedHitResult();
        
        if (modified != null) {
            FlexiblePlacementHandler.clearModifiedHitResult();
            PiggyBuildClient.LOGGER.info("[Mixin] Applied modified hit result");
            return modified;
        }
        
        return original;
    }
}