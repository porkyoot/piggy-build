package is.pig.minecraft.build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import is.pig.minecraft.build.config.PiggyConfig;
import is.pig.minecraft.build.mvc.controller.InputController;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import is.pig.minecraft.build.mvc.view.FlexiblePlacementRenderer;
import is.pig.minecraft.build.mvc.view.HighlightRenderType;
import is.pig.minecraft.build.mvc.view.WorldShapeRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PiggyBuildClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("piggy-build");

    // The controller manages input (keyboard, mouse, tick)
    private final InputController controller = new InputController();

    @Override
    public void onInitializeClient() {
        // 1. Load configuration
        PiggyConfig.load();

        // 2. Initialize controller
        controller.initialize();

        // 3. Render loop (visualization)
        WorldRenderEvents.LAST.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack stack = context.matrixStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

            // --- PART 1: RENDER BUILD SHAPES (Circle, Sphere...) ---
            renderBuildShapes(mc, cameraPos, stack, buffers);

            // --- PART 2: FLEXIBLE PLACEMENT (face overlay) ---
            renderFlexiblePlacement(mc, cameraPos, stack, buffers);
        });
    }

    /**
     * Handles rendering of the fixed shape (BuildSession).
     */
    private void renderBuildShapes(Minecraft mc, Vec3 cameraPos, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        BuildSession session = BuildSession.getInstance();
        
        // If no anchor position is set, don't draw any shape
        if (!session.isActive()) return;

        VertexConsumer builder = buffers.getBuffer(HighlightRenderType.TYPE);

        // Retrieve colors from the config
        PiggyConfig config = PiggyConfig.getInstance();
        float r = config.getRedFloat();
        float g = config.getGreenFloat();
        float b = config.getBlueFloat();
        float a = config.getAlphaFloat();

        // Calculate position relative to the camera
        double rx = session.getAnchorPos().getX() - cameraPos.x;
        double ry = session.getAnchorPos().getY() - cameraPos.y;
        double rz = session.getAnchorPos().getZ() - cameraPos.z;

        stack.pushPose();
        stack.translate(rx, ry, rz);

        // Delegate to geometric renderer
        switch (session.getShape()) {
            case BLOCK -> WorldShapeRenderer.drawBlock(builder, stack.last().pose(), 0, 0, 0, r, g, b, a);
            case LINE -> WorldShapeRenderer.drawLine(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
            case SPHERE -> WorldShapeRenderer.drawSphere(builder, stack.last().pose(), session.getRadius(), r, g, b, a);
            case RING -> WorldShapeRenderer.drawRing(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
        }

        stack.popPose();
        // Send the batch for transparency
        buffers.endBatch(HighlightRenderType.TYPE);
    }

    /**
     * Handles rendering of the placement overlay (PlacementSession).
     */
    private void renderFlexiblePlacement(Minecraft mc, Vec3 cameraPos, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        PlacementSession session = PlacementSession.getInstance();

        // Only check active, offset CAN be null now (Center)
        if (!session.isActive()) return;

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.hitResult;
            Direction offset = session.getCurrentOffset();

            // SELECT TEXTURE
            // If offset is null -> Center Texture
            // If offset is Direction -> Arrow Texture
            net.minecraft.resources.ResourceLocation tex = (offset == null) 
                ? FlexiblePlacementRenderer.getCenterTexture() 
                : FlexiblePlacementRenderer.getArrowTexture();

            // Get specific buffer for this texture
            VertexConsumer overlayBuilder = buffers.getBuffer(FlexiblePlacementRenderer.getRenderType(tex));

            double rx = hit.getBlockPos().getX() - cameraPos.x;
            double ry = hit.getBlockPos().getY() - cameraPos.y;
            double rz = hit.getBlockPos().getZ() - cameraPos.z;

            stack.pushPose();
            stack.translate(rx, ry, rz);

            // Pass configured overlay color (placement-specific)
            PiggyConfig config = PiggyConfig.getInstance();
            float rr = config.getPlacementRedFloat();
            float gg = config.getPlacementGreenFloat();
            float bb = config.getPlacementBlueFloat();
            float aa = config.getPlacementAlphaFloat();

            FlexiblePlacementRenderer.render(
                overlayBuilder,
                stack,
                hit.getDirection(),
                offset, // Can be null
                rr, gg, bb, aa
            );

            stack.popPose();
            
            // End specific batch
            buffers.endBatch(FlexiblePlacementRenderer.getRenderType(tex));
        }
    }
}