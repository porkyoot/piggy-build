package is.pig.minecraft.build;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PiggyBuildClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.LAST.register(context -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
                return;
            }

            BlockHitResult hit = (BlockHitResult) minecraft.hitResult;
            BlockPos pos = hit.getBlockPos();

            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();

            PoseStack poseStack = context.matrixStack();
            poseStack.pushPose();
            //poseStack.translate(x, y, z);

            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            // 1. Fill with texture
            VertexConsumer builderFill = bufferSource.getBuffer(HighlightRenderTypes.HIGHLIGHT_TYPE);
            //Matrix4f mat = poseStack.last().pose();
            
            // 2. Tinted cyan
			float r = 0f, g = 1f, b = 0.9f, a = 0.4f;
			int radius = 4;
            
			//BoxRenderer.drawBoxFill(builderFill, mat, r, g, b, a);
			
			// --- BOUCLE DE DESSIN ---
            // On scanne un carré autour du centre
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    
                    double distanceSq = (x * x) + (z * z);
                    
                    // 1. Rayon Extérieur (Le bord max)
                    double maxDist = radius * radius;
                    
                    // 2. Rayon Intérieur (Le trou au milieu)
                    // Si on enlève 1.0, on aura un cercle d'environ 1 bloc d'épaisseur.
                    // Si tu veux un cercle plus épais, mets (radius - 2.0).
                    double innerRadius = radius - 1.0;
                    if (innerRadius < 0) innerRadius = 0; // Sécurité
                    double minDist = innerRadius * innerRadius;

                    // LOGIQUE CERCLE :
                    // On dessine si on est PLUS PETIT que le max ET PLUS GRAND que le min
                    if (distanceSq <= maxDist && distanceSq > minDist) {
                        
                        double renderX = (pos.getX() + x) - cameraPos.x;
                        double renderY = pos.getY() - cameraPos.y;
                        double renderZ = (pos.getZ() + z) - cameraPos.z;

                        poseStack.pushPose();
                        poseStack.translate(renderX, renderY, renderZ);
                        
                        Matrix4f mat = poseStack.last().pose();

                        // On dessine la boite pour ce bloc
                        BoxRenderer.drawBoxFill(builderFill, mat, r, g, b, a);

                        poseStack.popPose();
                    }
                }
            }

            bufferSource.endBatch(HighlightRenderTypes.HIGHLIGHT_TYPE);
            bufferSource.endBatch(RenderType.lines());

            poseStack.popPose();
        });
    }
}