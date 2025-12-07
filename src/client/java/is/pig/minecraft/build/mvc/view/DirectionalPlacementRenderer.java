package is.pig.minecraft.build.mvc.view;

import com.mojang.blaze3d.vertex.*;

import is.pig.minecraft.build.lib.math.PlacementCalculator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class DirectionalPlacementRenderer {

    // Textures
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath("piggy-build",
            "textures/misc/placement_overlay.png");
    private static final ResourceLocation CENTER_TEXTURE = ResourceLocation.fromNamespaceAndPath("piggy-build",
            "textures/misc/placement_center.png");

    private static final float EPSILON = 0.005f;

    // Default overlay color (white) — configurable at runtime
    private static float DEFAULT_R = 1.0f;
    private static float DEFAULT_G = 1.0f;
    private static float DEFAULT_B = 1.0f;
    private static float DEFAULT_A = 1.0f;

    // We need a RenderType that allows changing textures dynamically or create two
    // RenderTypes.
    // Since RenderType.create is static/immutable regarding texture, we will create
    // a helper method
    // to get the RenderType for a specific texture.
    public static RenderType getRenderType(ResourceLocation texture) {
        return RenderType.create("piggy_placement_" + texture.getPath(),
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }

    /**
     * @param offset can be NULL (Center) or a Direction (Edge)
     */
    public static void render(VertexConsumer builder, PoseStack stack, Direction face, Direction offset) {
        render(builder, stack, face, offset, DEFAULT_R, DEFAULT_G, DEFAULT_B, DEFAULT_A);
    }

    /**
     * Render overlay with explicit color components.
     * 
     * @param r red 0..1
     * @param g green 0..1
     * @param b blue 0..1
     * @param a alpha 0..1
     */
    public static void render(VertexConsumer builder, PoseStack stack, Direction face, Direction offset, float r,
            float g, float b, float a) {
        if (face == null)
            return;

        stack.pushPose();

        // 1. Center on block
        stack.translate(0.5f, 0.5f, 0.5f);

        // 2. Rotate to match face normal
        Quaternionf faceRotation = face.getRotation();
        stack.mulPose(faceRotation);

        // 3. Push out slightly
        stack.translate(0, 0.5f + EPSILON, 0);

        // 4. Handle Rotation vs Center
        if (offset != null) {
            // EDGE CASE: Rotate arrow
            float rotationAngle = PlacementCalculator.getTextureRotation(face, offset);
            stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationAngle));
        } else {
            // CENTER CASE: No rotation needed (or fixed rotation)
            // The texture will be displayed flat on the face
        }

        // 5. Draw Quad
        Matrix4f mat = stack.last().pose();
        float size = 0.5f;
        float min = -size;
        float max = size;

        // Note: The builder MUST correspond to the texture used (handled in Client)
        builder.addVertex(mat, min, 0, min).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, min, 0, max).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, max, 0, max).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, max, 0, min).setUv(1, 0).setColor(r, g, b, a);

        stack.popPose();
    }

    // Public accessors for textures (used in Client)
    public static ResourceLocation getArrowTexture() {
        return ARROW_TEXTURE;
    }

    public static ResourceLocation getCenterTexture() {
        return CENTER_TEXTURE;
    }
}