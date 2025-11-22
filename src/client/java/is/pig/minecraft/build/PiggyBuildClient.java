package is.pig.minecraft.build;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import is.pig.minecraft.build.config.PiggyConfig; // <--- IMPORT
import is.pig.minecraft.build.mvc.controller.InputController;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.view.HighlightRenderType;
import is.pig.minecraft.build.mvc.view.WorldShapeRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class PiggyBuildClient implements ClientModInitializer {

    private final InputController controller = new InputController();

    @Override
    public void onInitializeClient() {
        // 1. Initialize Controller (Inputs)
        controller.initialize();
        
        // 2. Load Config
        PiggyConfig.load();

        // 3. Register Renderer (View)
        WorldRenderEvents.LAST.register(context -> {
            BuildSession session = BuildSession.getInstance();
            if (!session.isActive()) return;

            Minecraft mc = Minecraft.getInstance();
            Vec3 cameraPos = context.camera().getPosition();
            PoseStack stack = context.matrixStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer builder = buffers.getBuffer(HighlightRenderType.TYPE);

            // --- USE CONFIG HERE ---
            PiggyConfig config = PiggyConfig.getInstance();
            float r = config.getRedFloat();
            float g = config.getGreenFloat();
            float b = config.getBlueFloat();
            float a = config.getAlphaFloat();

            // Calculate relative position
            double rx = session.getAnchorPos().getX() - cameraPos.x;
            double ry = session.getAnchorPos().getY() - cameraPos.y;
            double rz = session.getAnchorPos().getZ() - cameraPos.z;

            stack.pushPose();
            stack.translate(rx, ry, rz);

            // Delegate to ShapeRenderer
            switch (session.getShape()) {
                case BLOCK -> WorldShapeRenderer.drawBlock(builder, stack.last().pose(), 0, 0, 0, r, g, b, a);
                case LINE -> WorldShapeRenderer.drawLine(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
                case SPHERE -> WorldShapeRenderer.drawSphere(builder, stack.last().pose(), session.getRadius(), r, g, b, a);
                case RING -> WorldShapeRenderer.drawRing(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
            }

            stack.popPose();
            buffers.endBatch(HighlightRenderType.TYPE);
        });
    }
}