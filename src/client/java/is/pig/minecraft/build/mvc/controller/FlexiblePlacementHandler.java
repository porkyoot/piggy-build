package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import is.pig.minecraft.build.lib.math.PlacementCalculator;
import is.pig.minecraft.build.mixin.client.MinecraftAccessorMixin;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class FlexiblePlacementHandler {

    private static final Logger LOGGER = PiggyBuildClient.LOGGER;
    private boolean isPlacing = false;

    public void onTick(Minecraft client) {
        boolean isFlexDown = InputController.flexibleKey.isDown();
        PlacementSession.getInstance().setActive(isFlexDown);

        if (isFlexDown) {
            if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                Direction offset = PlacementCalculator.getOffsetDirection(hit);
                PlacementSession.getInstance().setCurrentOffset(offset);
            } else {
                PlacementSession.getInstance().setCurrentOffset(null);
            }
        }
    }

    public InteractionResult onUseBlock(Minecraft client, InteractionHand hand, BlockHitResult hitResult) {
        // 1. IMPORTANT : On ne touche à rien côté serveur !
        // UseBlockCallback est appelé sur les deux côtés. On veut agir uniquement sur le client
        // pour envoyer un paquet de placement modifié.
        if (client.level != null && !client.level.isClientSide) {
            return InteractionResult.PASS;
        }

        // 2. Protection Récursion
        if (this.isPlacing) return InteractionResult.PASS;

        // 3. Vérifications Basiques
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!InputController.flexibleKey.isDown()) return InteractionResult.PASS;

        Direction offset = PlacementCalculator.getOffsetDirection(hitResult);
        if (offset == null) return InteractionResult.PASS; // Centre -> Vanilla

        // --- LOGIQUE DE PLACEMENT FORCÉ ---
        
        // On affiche le délai pour info, mais on ne s'arrête plus à cause de lui
        // LOGGER.info("Délai avant forçage : " + ((MinecraftAccessorMixin) client).getRightClickDelay());

        BlockPos pos = hitResult.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos);
        
        // Calcul précis du point d'impact sur la nouvelle face (Offset)
        Vec3 newHitPos = center.add(
            offset.getStepX() * 0.5,
            offset.getStepY() * 0.5,
            offset.getStepZ() * 0.5
        );

        BlockHitResult newHit = new BlockHitResult(newHitPos, offset, pos, hitResult.isInside());

        try {
            this.isPlacing = true;
            
            // LE FIX : On écrase le délai. On force le passage.
            ((MinecraftAccessorMixin) client).setRightClickDelay(0);

            // On exécute l'action avec notre Faux Clic
            InteractionResult result = client.gameMode.useItemOn(client.player, hand, newHit);

            if (result.consumesAction()) {
                if (result.shouldSwing()) client.player.swing(hand);
                
                // Succès : On remet le délai à 4 pour éviter le spam APRÈS notre action
                ((MinecraftAccessorMixin) client).setRightClickDelay(4);
                
                return InteractionResult.SUCCESS; // Stop Vanilla
            }
            
        } catch (Exception e) {
            LOGGER.error("Erreur placement flexible", e);
        } finally {
            this.isPlacing = false;
        }

        // Si ça a raté (ex: bloc dans le chemin), on met quand même le délai
        // et on renvoie FAIL pour empêcher le clic Vanilla sur la face d'origine.
        ((MinecraftAccessorMixin) client).setRightClickDelay(4);
        return InteractionResult.FAIL; 
    }
}