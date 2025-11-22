package is.pig.minecraft.build;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

    // On met la touche en public static pour y accéder depuis le Screen
    public static KeyMapping triggerKey;
    
	private static BlockPos frozenPos = null;
	private static Direction.Axis frozenAxis = Direction.Axis.Y;
    private static double currentRadius = 4.0;
    
    // NOUVEAU : La forme actuelle
    private static BuildShape currentShape = BuildShape.RING;
    
    private boolean wasKeyDown = false;

    // GETTER
    public static BuildShape getShape() {
        return currentShape;
    }

    // SETTER (Silencieux pour le temps réel)
    public static void setShape(BuildShape shape) {
        currentShape = shape;
    }
    
    public static double getCurrentRadius() {
        return currentRadius;
    }
	
	public static void modifyRadius(int amount) {
        currentRadius += amount;
        if (currentRadius < 1.0) currentRadius = 1.0;
        if (currentRadius > 64.0) currentRadius = 64.0;
        
        // Feedback
        Minecraft.getInstance().player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("Rayon : " + (int)currentRadius), true
        );
    }

    @Override
    public void onInitializeClient() {
        // 1. Touche X
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.piggy_build.trigger", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.piggy_build"
        ));

        // 2. MOLETTE (Avec ton Mixin existant)
        MouseScrollCallback.EVENT.register((amount) -> {
            if (triggerKey.isDown()) {
                if (amount > 0) currentRadius += 1.0;
                else if (amount < 0) currentRadius -= 1.0;
                if (currentRadius < 1.0) currentRadius = 1.0;
                if (currentRadius > 128.0) currentRadius = 128.0;
                
                Minecraft.getInstance().player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Rayon : " + (int)currentRadius), true
                );
                return true; 
            }
            return false;
        });

       // 3. LOGIQUE TICK : OUVERTURE DU MENU
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = triggerKey.isDown();

            // On ouvre SEULEMENT si la touche vient d'être appuyée ET qu'aucun menu n'est ouvert
            if (isKeyDown && !wasKeyDown && client.screen == null) {
                BlockHitResult hit = (BlockHitResult) client.hitResult;
                // Capture de la position
				if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
					
					frozenPos = hit.getBlockPos();
					frozenAxis = hit.getDirection().getAxis();
                }

                // Ouverture du menu
                client.setScreen(new RadialMenuScreen(currentShape));
            }
            
            wasKeyDown = isKeyDown;
        });

        // 4. RENDU MONDE
        WorldRenderEvents.LAST.register(context -> {
            if (frozenPos == null) return;

            Minecraft minecraft = Minecraft.getInstance();
            // Si le menu radial est ouvert, on dessine quand même la forme en temps réel
            // pour que le joueur voie ce qu'il sélectionne.
            
            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack poseStack = context.matrixStack();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            VertexConsumer builderFill = bufferSource.getBuffer(HighlightRenderTypes.HIGHLIGHT_TYPE);
            
            float r = 0f, g = 1f, b = 0.9f, a = 0.4f;

            double renderX = frozenPos.getX() - cameraPos.x;
            double renderY = frozenPos.getY() - cameraPos.y;
            double renderZ = frozenPos.getZ() - cameraPos.z;

            poseStack.pushPose();
            poseStack.translate(renderX, renderY, renderZ);
            Matrix4f mat = poseStack.last().pose();
            
            // CHOIX DU DESSIN SELON L'ENUM
            switch (currentShape) {
                case BLOCK:
                    // Juste un bloc au centre (0,0,0)
                    ShapeRenderer.drawBlock(builderFill, mat, 0, 0, 0, r, g, b, a);
                    break;
                case LINE:
                    ShapeRenderer.drawLine(builderFill, mat, frozenAxis, currentRadius, r, g, b, a);
                    break;
                case SPHERE:
                    ShapeRenderer.drawSphere(builderFill, mat, currentRadius, r, g, b, a);
                    break;
                case RING:
                default:
                    ShapeRenderer.drawRing(builderFill, mat, frozenAxis, currentRadius, r, g, b, a);
                    break;
            }

            poseStack.popPose();
            bufferSource.endBatch(HighlightRenderTypes.HIGHLIGHT_TYPE);
        });
    }
}