package is.pig.minecraft.build.mvc.view;

import is.pig.minecraft.build.config.PiggyBuildConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

/**
 * HUD overlay that displays when Fast Placement mode is active.
 * Renders in attack indicator style - below the crosshair with a double arrow
 * icon.
 */
public class FastPlaceOverlay {

    private static final ResourceLocation SPEED_ICON = ResourceLocation.fromNamespaceAndPath("piggy-build",
            "textures/gui/speed_indicator.png");
    private static final int ICON_SIZE = 16;

    private static Boolean lastState = null; // Use Boolean to detect first render
    private static long lastPlaceTime = 0;

    /**
     * Call this when a fast placement occurs to track cooldown
     */
    public static void onFastPlace() {
        lastPlaceTime = System.currentTimeMillis();
    }

    /**
     * Render the fast placement indicator on the HUD
     */
    public static void render(GuiGraphics graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        boolean isActive = PiggyBuildConfig.getInstance().isFastPlaceEnabled();

        // Initialize lastState on first render to match config
        if (lastState == null) {
            lastState = isActive;
        }

        // Track state changes
        if (isActive != lastState) {
            lastState = isActive;
        }

        if (!isActive) {
            return;
        }

        // Always show indicator when active (attack indicator style)
        renderAttackIndicatorStyle(graphics, tickDelta);
    }

    /**
     * Render in attack indicator style - below crosshair with double arrow
     */
    private static void renderAttackIndicatorStyle(GuiGraphics graphics, DeltaTracker tickDelta) {
        // Calculate progress (0.0 to 1.0) based on cooldown
        int cps = PiggyBuildConfig.getInstance().getTickDelay();
        int delayMs = cps > 0 ? 1000 / cps : 0;
        long timeSincePlace = System.currentTimeMillis() - lastPlaceTime;
        float progress = delayMs <= 0 ? 1.0f : Math.min(1.0f, timeSincePlace / (float) delayMs);

        // Position below crosshair (similar to attack indicator)
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Offset below crosshair - centered
        int indicatorX = centerX - ICON_SIZE / 2;
        int indicatorY = centerY + 10; // Below crosshair

        // Set color with alpha based on cooldown progress
        float alpha = progress >= 1.0f ? 1.0f : 0.67f; // Full opacity when ready, slightly transparent when cooling

        // Use custom RenderType for inversion (similar to crosshair)
        // We use GUI_GHOSTRECIPE_LINK_OR_INVERTED or similar standard inversion state if available,
        // but creating a specific one ensures it works as intended.
        net.minecraft.client.renderer.RenderType invertedType = net.minecraft.client.renderer.RenderType.create(
            "piggy_gui_inverted",
            com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
            1536,
            false,
            false,
            net.minecraft.client.renderer.RenderType.CompositeState.builder()
                .setShaderState(new net.minecraft.client.renderer.RenderStateShard.ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
                .setTextureState(new net.minecraft.client.renderer.RenderStateShard.TextureStateShard(SPEED_ICON, false, false))
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

        float x1 = (float) indicatorX;
        float y1 = (float) indicatorY;
        float x2 = (float) (indicatorX + ICON_SIZE);
        float y2 = (float) (indicatorY + ICON_SIZE);

        buffer.addVertex(matrix, x1, y1, 0).setUv(0, 0).setColor(1.0f, 1.0f, 1.0f, alpha);
        buffer.addVertex(matrix, x1, y2, 0).setUv(0, 1).setColor(1.0f, 1.0f, 1.0f, alpha);
        buffer.addVertex(matrix, x2, y2, 0).setUv(1, 1).setColor(1.0f, 1.0f, 1.0f, alpha);
        buffer.addVertex(matrix, x2, y1, 0).setUv(1, 0).setColor(1.0f, 1.0f, 1.0f, alpha);

        // Force the buffer to draw immediately to ensure correct layering in GUI
        graphics.flush();
    }

}