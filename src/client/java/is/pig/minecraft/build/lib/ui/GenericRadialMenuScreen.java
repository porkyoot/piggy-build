package is.pig.minecraft.build.lib.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import is.pig.minecraft.build.config.PiggyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A generic Radial Menu Screen that displays a center item and a ring of surrounding items.
 * @param <T> The type of the item (usually an Enum).
 */
public class GenericRadialMenuScreen<T extends RadialMenuItem> extends Screen {

    // Constants for UI sizing
    private static final int ICON_SIZE = 16;
    private static final float INNER_RADIUS = 12f;
    private static final float OUTER_RADIUS = 34f;
    private static final float ICON_DISTANCE = 24f;

    private final T centerItem;
    private final List<T> radialItems;
    private final Consumer<T> onSelectionChanged; // Callback when selection changes
    private final Function<T, Component> extraInfoProvider;
    private final Runnable onCloseCallback;       // Callback when menu closes
    private final InputConstants.Key triggerKey; // The key holding the menu open
    private final Predicate<Double> onScrollCallback;

    private T selectedItem;

    public GenericRadialMenuScreen(Component title, 
                                   T centerItem, 
                                   List<T> radialItems, 
                                   T currentSelection, 
                                   InputConstants.Key triggerKey,
                                   Consumer<T> onSelectionChanged,
                                   Runnable onCloseCallback,
                                   Function<T, Component> extraInfoProvider,
            Predicate<Double> onScrollCallback) {
        super(title);
        this.centerItem = centerItem;
        this.radialItems = radialItems;
        this.selectedItem = currentSelection;
        this.triggerKey = triggerKey;
        this.onSelectionChanged = onSelectionChanged;
        this.onCloseCallback = onCloseCallback;
        this.extraInfoProvider = extraInfoProvider;
        this.onScrollCallback = onScrollCallback;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // If we're on the center item, do nothing (as requested earlier)
        if (selectedItem == centerItem) {
            return false;
        }

        // If a scroll callback is defined and there is movement
        if (onScrollCallback != null && scrollY != 0) {
            // Call the controller. If it returns true, consider the event handled.
            if (onScrollCallback.test(scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        if (!isTriggerKeyPressed()) {
            this.onClose();
        }
    }

    private boolean isTriggerKeyPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (triggerKey.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, triggerKey.getValue());
        } else if (triggerKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, triggerKey.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (onCloseCallback != null) onCloseCallback.run();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        updateSelection(mouseX, mouseY, cx, cy);
        renderBackgroundGeometry(graphics, cx, cy);
        renderIcons(graphics, cx, cy);
    }

    private void updateSelection(int mx, int my, int cx, int cy) {
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx*dx + dy*dy);

        T newSelection;

        if (dist < INNER_RADIUS) {
            newSelection = centerItem;
        } else {
            double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
            if (angle < 0) angle += 2 * Math.PI;

            double anglePerItem = (2 * Math.PI) / radialItems.size();
            int index = (int) (angle / anglePerItem) % radialItems.size();
            newSelection = radialItems.get(index);
        }

        // Notify controller if selection changed
        if (newSelection != selectedItem) {
            selectedItem = newSelection;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(selectedItem);
            }
        }
    }

    private void renderBackgroundGeometry(GuiGraphics graphics, int cx, int cy) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 10); // Z-offset
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = poseStack.last().pose();

        // Render Radial Segments
        double anglePerItem = (2 * Math.PI) / radialItems.size();
        for (int i = 0; i < radialItems.size(); i++) {
            // Colors could be extracted to a Config object in the future
            float r = 1f; 
            float g = 1f; 
            float b = 1f; 
            float a = 0.3f;
            if ((radialItems.get(i) == selectedItem)) { // if hovered
             PiggyConfig config = PiggyConfig.getInstance();
             r = config.getRedFloat();
             g = config.getGreenFloat();
             b = config.getBlueFloat();
             a = 0.6f;
            }

            double start = (i * anglePerItem) - Math.toRadians(90);
            double end = ((i + 1) * anglePerItem) - Math.toRadians(90);
            double gap = Math.toRadians(2);

            drawArc(buffer, mat, cx, cy, INNER_RADIUS + 2, OUTER_RADIUS, start + gap, end - gap, r, g, b, a);
        }

        // Render Center
        //boolean centerHovered = (centerItem == selectedItem);
        //float cr = centerHovered ? 0f : 1f; 
        //float cg = centerHovered ? 1f : 1f; 
        //float cb = centerHovered ? 0.9f : 1f; 
        //float ca = centerHovered ? 0.5f : 0.15f;
        
        //drawArc(buffer, mat, cx, cy, 0, INNER_RADIUS - 2, 0, Math.PI * 2, cr, cg, cb, ca);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private void renderIcons(GuiGraphics graphics, int cx, int cy) {
        // Radial Icons
        double anglePerItem = (2 * Math.PI) / radialItems.size();
        for (int i = 0; i < radialItems.size(); i++) {
            T item = radialItems.get(i);
            double midAngle = (i * anglePerItem + (anglePerItem / 2)) - Math.toRadians(90);
            int x = (int) (cx + Math.cos(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);
            int y = (int) (cy + Math.sin(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);

            boolean isSelected = (item == selectedItem);
            drawItemIcon(graphics, item, x, y);

            if (isSelected && extraInfoProvider != null) {
                Component info = extraInfoProvider.apply(item);
                if (info != null) {
                    drawExtraInfo(graphics, info, x, y, midAngle);
                }
            }
        }

        // Center Icon
        drawItemIcon(graphics, centerItem, cx - (ICON_SIZE / 2), cy - (ICON_SIZE / 2));
    }
    
    private void drawExtraInfo(GuiGraphics graphics, Component text, int iconX, int iconY, double angleRad) {
        float textDistance = ICON_SIZE * 0.8f; 
        int textX = iconX + (ICON_SIZE / 2) + (int)(Math.cos(angleRad) * textDistance);
        int textY = iconY + (ICON_SIZE / 2) + (int)(Math.sin(angleRad) * textDistance);

        int textWidth = this.font.width(text);
        textX -= textWidth / 2;
        textY -= this.font.lineHeight / 2;

        graphics.drawString(this.font, text, textX, textY, 0xFFFFFF, true);
    }

    private void drawItemIcon(GuiGraphics graphics, T item, int x, int y) {
        boolean selected = (item == selectedItem);
        RenderSystem.enableBlend();
        
        if(selected) {
             // Use Config Color for Tinting
             PiggyConfig config = PiggyConfig.getInstance();
             RenderSystem.setShaderColor(
                 config.getRedFloat(), 
                 config.getGreenFloat(), 
                 config.getBlueFloat(), 
                 1.0f // Keep icon fully opaque, just tinted
             );
        } else {
             RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        
        graphics.blit(item.getIconLocation(selected), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawArc(VertexConsumer buffer, Matrix4f mat, float cx, float cy, float rIn, float rOut, double start, double end, float r, float g, float b, float a) {
        int segments = 32;
        double step = (end - start) / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = start + (i * step);
            double a2 = start + ((i + 1) * step);
            
            // Vertices calculation
            float x1In = (float)(cx + Math.cos(a1)*rIn); float y1In = (float)(cy + Math.sin(a1)*rIn);
            float x1Out = (float)(cx + Math.cos(a1)*rOut); float y1Out = (float)(cy + Math.sin(a1)*rOut);
            float x2In = (float)(cx + Math.cos(a2)*rIn); float y2In = (float)(cy + Math.sin(a2)*rIn);
            float x2Out = (float)(cx + Math.cos(a2)*rOut); float y2Out = (float)(cy + Math.sin(a2)*rOut);

            // Quad (2 Triangles)
            buffer.addVertex(mat, x1In, y1In, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x1Out, y1Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2In, y2In, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x1Out, y1Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2Out, y2Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2In, y2In, 0).setColor(r, g, b, a);
        }
    }
}