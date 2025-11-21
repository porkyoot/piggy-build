package is.pig.minecraft.build;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;

public class PiggyBuildClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.LAST.register(context -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.hitResult == null || minecraft.hitResult.getType() != HitResult.Type.BLOCK) {
                return;
            }

            BlockHitResult hit = (BlockHitResult) minecraft.hitResult;
            BlockPos pos = hit.getBlockPos(); // Le centre

            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack poseStack = context.matrixStack();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            // 1. Récupération du buffer (avec Texture ou juste couleur selon ton HighlightRenderTypes)
            VertexConsumer builderFill = bufferSource.getBuffer(HighlightRenderTypes.HIGHLIGHT_TYPE);
            
            // 2. Configuration
            float r = 0f, g = 1f, b = 0.9f, a = 0.4f;
            double radius = 4.0;

            // 3. Positionnement au CENTRE du bloc visé
            // On se place une seule fois, et ShapeRenderer dessine autour (offsets relatifs)
            double renderX = pos.getX() - cameraPos.x;
            double renderY = pos.getY() - cameraPos.y;
            double renderZ = pos.getZ() - cameraPos.z;

            poseStack.pushPose();
            poseStack.translate(renderX, renderY, renderZ);

            // 4. Appel magique
            Matrix4f mat = poseStack.last().pose();
            ShapeRenderer.drawRing(builderFill, mat, Direction.Axis.X, radius, r, g, b, a);

            poseStack.popPose();

            // Fin du batch
            bufferSource.endBatch(HighlightRenderTypes.HIGHLIGHT_TYPE);
        });
    }
}