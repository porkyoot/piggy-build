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

    public static KeyMapping triggerKey;
    
    private static BlockPos frozenPos = null;
    private static Direction.Axis frozenAxis = Direction.Axis.Y;
    private static double currentRadius = 4.0;
    private static BuildShape currentShape = BuildShape.RING;
    private boolean wasKeyDown = false;

    // --- CONFIGURATION COULEUR ---
    public static float HIGHLIGHT_RED = 0.0f;
    public static float HIGHLIGHT_GREEN = 1.0f;
    public static float HIGHLIGHT_BLUE = 0.9f;
    public static float HIGHLIGHT_ALPHA = 0.4f; 

    // --- GETTERS / SETTERS ---
    public static BuildShape getShape() { return currentShape; }
    
    public static void setShape(BuildShape shape) { 
        currentShape = shape; 
        // PAS de popup ici
    }
    
    public static double getCurrentRadius() { return currentRadius; }
    
    public static void modifyRadius(int amount) {
        currentRadius += amount;
        if (currentRadius < 1.0) currentRadius = 1.0;
        if (currentRadius > 64.0) currentRadius = 64.0;
        
        // PAS de popup ici
    }

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.piggy_build.trigger", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, "category.piggy_build"
        ));

        MouseScrollCallback.EVENT.register((amount) -> {
            if (triggerKey.isDown()) {
                modifyRadius(amount > 0 ? 1 : -1);
                return true; 
            }
            return false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            boolean isKeyDown = triggerKey.isDown();

            if (isKeyDown && !wasKeyDown && client.screen == null) {
                if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) client.hitResult;
                    frozenPos = hit.getBlockPos();
                    frozenAxis = hit.getDirection().getAxis();
                }
                client.setScreen(new RadialMenuScreen(currentShape));
            }
            wasKeyDown = isKeyDown;
        });

        WorldRenderEvents.LAST.register(context -> {
            if (frozenPos == null) return;

            Minecraft minecraft = Minecraft.getInstance();
            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack poseStack = context.matrixStack();
            MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

            VertexConsumer builderFill = bufferSource.getBuffer(HighlightRenderTypes.HIGHLIGHT_TYPE);
            
            float r = HIGHLIGHT_RED;
            float g = HIGHLIGHT_GREEN;
            float b = HIGHLIGHT_BLUE;
            float a = HIGHLIGHT_ALPHA;

            double renderX = frozenPos.getX() - cameraPos.x;
            double renderY = frozenPos.getY() - cameraPos.y;
            double renderZ = frozenPos.getZ() - cameraPos.z;

            poseStack.pushPose();
            poseStack.translate(renderX, renderY, renderZ);
            Matrix4f mat = poseStack.last().pose();
            
            switch (currentShape) {
                case BLOCK:
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