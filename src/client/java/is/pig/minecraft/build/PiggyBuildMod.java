package is.pig.minecraft.build;

import is.pig.minecraft.build.mvc.controller.InputController;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.view.HighlightRenderType;
import is.pig.minecraft.build.mvc.view.WorldShapeRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

public class PiggyBuildMod implements ClientModInitializer {

    private final InputController controller = new InputController();

    @Override
    public void onInitializeClient() {
        // 1. Initialize Controller (Inputs)
        controller.initialize();

        // 2. Register Renderer (View)
        WorldRenderEvents.LAST.register(context -> {
            BuildSession session = BuildSession.getInstance();
            if (!session.isActive()) return;

            Minecraft mc = Minecraft.getInstance();
            Vec3 cameraPos = context.camera().getPosition();
            var stack = context.matrixStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer builder = buffers.getBuffer(HighlightRenderType.TYPE);

            // Config Colors (Teal)
            float r = 0f, g = 1f, b = 0.9f, a = 0.4f;

            // Calculate relative position
            double rx = session.getAnchorPos().getX() - cameraPos.x;
            double ry = session.getAnchorPos().getY() - cameraPos.y;
            double rz = session.getAnchorPos().getZ() - cameraPos.z;

            stack.pushPose();
            stack.translate(rx, ry, rz);

            // Delegate to ShapeRenderer based on State
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