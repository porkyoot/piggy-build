package is.pig.minecraft.build;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents; // <--- IMPORT IMPORTANT
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiggyBuildClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("piggy-build");

    private static KeyMapping triggerKey;
    
    // On rend cette variable 'volatile' pour éviter des soucis entre les Threads (Logic vs Render)
    private static volatile BlockPos frozenPos = null;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Ehlo from Piggy Build !");

        // 1. Enregistrement de la touche
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.piggy_build.trigger",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X, 
            "category.piggy_build"
        ));

        // 2. BOUCLE LOGIQUE (20 fois par seconde)
        // C'est ici qu'on gère les Inputs proprement
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Si on est en jeu et que la touche est appuyée
            if (client.player != null && triggerKey.isDown()) {
                
                // On regarde ce que le joueur vise
                if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) client.hitResult;
                    frozenPos = hit.getBlockPos();
                    
                    // Log toutes les 20 ticks (1 sec) pour ne pas spammer
                    if (client.player.tickCount % 20 == 0) {
                        LOGGER.info("INPUT DETECTÉ ! Position gelée en : " + frozenPos);
                    }
                }
            }
        });

        // 3. BOUCLE DE RENDU (Chaque frame)
        // Ici, on ne fait QUE dessiner la dernière position connue
        WorldRenderEvents.LAST.register(context -> {
            
            // Si aucune position n'est définie, on ne dessine rien
            if (frozenPos == null) {
                return;
            }

            Minecraft minecraft = Minecraft.getInstance();

            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack poseStack = context.matrixStack();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            VertexConsumer builderFill = bufferSource.getBuffer(HighlightRenderTypes.HIGHLIGHT_TYPE);
            
            float r = 0f, g = 1f, b = 0.9f, a = 0.4f;
            double radius = 4.0;
            Direction.Axis axis = Direction.Axis.Y;

            double renderX = frozenPos.getX() - cameraPos.x;
            double renderY = frozenPos.getY() - cameraPos.y;
            double renderZ = frozenPos.getZ() - cameraPos.z;

            poseStack.pushPose();
            poseStack.translate(renderX, renderY, renderZ);

            Matrix4f mat = poseStack.last().pose();
            
            ShapeRenderer.drawRing(builderFill, mat, axis, radius, r, g, b, a);

            poseStack.popPose();
            
            bufferSource.endBatch(HighlightRenderTypes.HIGHLIGHT_TYPE);
        });
    }
}