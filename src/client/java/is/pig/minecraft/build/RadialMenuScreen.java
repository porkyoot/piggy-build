package is.pig.minecraft.build;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {

    private BuildShape selectedShape;
    
    private final BuildShape centerShape = BuildShape.BLOCK;
    private final List<BuildShape> radialShapes = new ArrayList<>();
    private static final int ICON_SIZE = 32;

    public RadialMenuScreen(BuildShape currentShape) {
        super(Component.literal("Radial Menu"));
        this.selectedShape = currentShape;

        for (BuildShape shape : BuildShape.values()) {
            if (shape != centerShape) {
                radialShapes.add(shape);
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selectedShape == centerShape) return false;
        if (scrollY != 0) {
            PiggyBuildClient.modifyRadius(scrollY > 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        InputConstants.Key boundKey = KeyBindingHelper.getBoundKeyOf(PiggyBuildClient.triggerKey);
        long windowHandle = Minecraft.getInstance().getWindow().getWindow();
        boolean isPressed = false;
        
        if (boundKey.getType() == InputConstants.Type.KEYSYM) {
            isPressed = InputConstants.isKeyDown(windowHandle, boundKey.getValue());
        } else if (boundKey.getType() == InputConstants.Type.MOUSE) {
            isPressed = GLFW.glfwGetMouseButton(windowHandle, boundKey.getValue()) == GLFW.GLFW_PRESS;
        }

        if (!isPressed) {
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        
        // --- DETECTER LA SÉLECTION ---
        double centerZoneRadius = 24.0; 

        if (dist < centerZoneRadius) {
            selectedShape = centerShape;
        } else {
            double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
            if (angle < 0) angle += 2 * Math.PI;

            double anglePerItem = (2 * Math.PI) / radialShapes.size();
            int hoverIndex = (int) (angle / anglePerItem) % radialShapes.size();
            
            selectedShape = radialShapes.get(hoverIndex);
        }

        // --- MISE A JOUR DYNAMIQUE (LIVE UPDATE) ---
        // C'est ici qu'on change la forme dans le monde instantanément !
        if (PiggyBuildClient.getShape() != selectedShape) {
            PiggyBuildClient.setShape(selectedShape);
        }

        // --- RENDU DES ICONES (Reste inchangé) ---
        float orbitRadius = 40f; 
        double anglePerItem = (2 * Math.PI) / radialShapes.size();
        
        for (int i = 0; i < radialShapes.size(); i++) {
            BuildShape shape = radialShapes.get(i);
            boolean isSelected = (shape == selectedShape);
            
            double midAngle = (i * anglePerItem + (anglePerItem / 2)) - Math.toRadians(90);
            int iconX = (int) (centerX + Math.cos(midAngle) * orbitRadius) - (ICON_SIZE / 2);
            int iconY = (int) (centerY + Math.sin(midAngle) * orbitRadius) - (ICON_SIZE / 2);

            drawIcon(guiGraphics, shape, iconX, iconY, isSelected);

            if (isSelected) {
                drawRadiusValue(guiGraphics, iconX, iconY, midAngle);
            }
        }

        boolean isCenterSelected = (selectedShape == centerShape);
        int centerIconX = centerX - (ICON_SIZE / 2);
        int centerIconY = centerY - (ICON_SIZE / 2);
        drawIcon(guiGraphics, centerShape, centerIconX, centerIconY, isCenterSelected);
    }

    private void drawIcon(GuiGraphics guiGraphics, BuildShape shape, int x, int y, boolean isSelected) {
        var texture = shape.getIcon(isSelected);
        RenderSystem.enableBlend();
        guiGraphics.blit(texture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }

    private void drawRadiusValue(GuiGraphics guiGraphics, int iconX, int iconY, double angleRad) {
        String radiusText = String.valueOf((int)PiggyBuildClient.getCurrentRadius());
        float textDistance = ICON_SIZE * 0.4f; 
        int textX = iconX + (ICON_SIZE / 2) + (int)(Math.cos(angleRad) * textDistance);
        int textY = iconY + (ICON_SIZE / 2) + (int)(Math.sin(angleRad) * textDistance);

        int textWidth = this.font.width(radiusText);
        textX -= textWidth / 2;
        textY -= this.font.lineHeight / 2;

        guiGraphics.drawString(this.font, radiusText, textX, textY, 0xFFFFFF, true);
    }
}