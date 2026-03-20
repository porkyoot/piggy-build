package is.pig.minecraft.build.mvc.view;

import is.pig.minecraft.build.config.PiggyBuildConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

public class AutoParkourOverlay {

    // Using vanilla jump boost effect icon for the leap indicator
    private static final ResourceLocation LEAP_ICON = ResourceLocation.withDefaultNamespace("textures/mob_effect/jump_boost.png");
    private static final int ICON_SIZE = 18;

    public static void render(GuiGraphics graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!PiggyBuildConfig.getInstance().isFeatureAutoParkourEnabled()) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Position slightly to the left of crosshair
        int indicatorX = centerX - ICON_SIZE - 4;
        int indicatorY = centerY - ICON_SIZE / 2;

        net.minecraft.client.renderer.RenderType invertedType = net.minecraft.client.renderer.RenderType.create(
            "piggy_gui_inverted_leap",
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            net.minecraft.client.renderer.RenderType.CompositeState.builder()
                .setShaderState(new net.minecraft.client.renderer.RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(LEAP_ICON, false, false))
                .setTransparencyState(new net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard("gui_inverted_transparency", () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                        GlStateManager.SourceFactor.ONE,
                        GlStateManager.DestFactor.ZERO);
                }, () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                }))
                .createCompositeState(false)
        );

        com.mojang.blaze3d.vertex.VertexConsumer buffer = graphics.bufferSource().getBuffer(invertedType);
        org.joml.Matrix4f matrix = graphics.pose().last().pose();

        buffer.addVertex(matrix, indicatorX, indicatorY, 0).setUv(0, 0).setColor(1.0f, 1.0f, 1.0f, 0.67f);
        buffer.addVertex(matrix, indicatorX, indicatorY + ICON_SIZE, 0).setUv(0, 1).setColor(1.0f, 1.0f, 1.0f, 0.67f);
        buffer.addVertex(matrix, indicatorX + ICON_SIZE, indicatorY + ICON_SIZE, 0).setUv(1, 1).setColor(1.0f, 1.0f, 1.0f, 0.67f);
        buffer.addVertex(matrix, indicatorX + ICON_SIZE, indicatorY, 0).setUv(1, 0).setColor(1.0f, 1.0f, 1.0f, 0.67f);

        graphics.flush();
    }
}
