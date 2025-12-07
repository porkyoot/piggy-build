package is.pig.minecraft.build.mvc.view;

import com.mojang.blaze3d.systems.RenderSystem;
import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

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

        boolean isActive = PiggyConfig.getInstance().isFastPlaceEnabled();

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
        int delayMs = PiggyConfig.getInstance().getFastPlaceDelayMs();
        long timeSincePlace = System.currentTimeMillis() - lastPlaceTime;
        float progress = Math.min(1.0f, timeSincePlace / (float) delayMs);

        // Position below crosshair (similar to attack indicator)
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Offset below crosshair - centered
        int indicatorX = centerX - ICON_SIZE / 2;
        int indicatorY = centerY + 10; // Below crosshair

        // Set inverted color blend mode (like crosshair)
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_DST_COLOR, // srcRGB
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_COLOR, // dstRGB
                org.lwjgl.opengl.GL11.GL_ONE, // srcAlpha
                org.lwjgl.opengl.GL11.GL_ZERO // dstAlpha
        );

        // Set color with alpha based on cooldown progress
        float alpha = progress >= 1.0f ? 1.0f : 0.67f; // Full opacity when ready, slightly transparent when cooling
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // Draw the icon
        graphics.blit(SPEED_ICON, indicatorX, indicatorY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Restore default state
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }

}