package is.pig.minecraft.build;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RadialMenuScreen extends Screen {

    private BuildShape selectedShape;
    
    // On sépare les formes : le Centre vs les Radiales
    private final BuildShape centerShape = BuildShape.BLOCK;
    private final List<BuildShape> radialShapes = new ArrayList<>();

    public RadialMenuScreen(BuildShape currentShape) {
        super(Component.literal("Radial Menu"));
        this.selectedShape = currentShape;

        // On remplit la liste des formes radiales (Tout sauf BLOCK)
        for (BuildShape shape : BuildShape.values()) {
            if (shape != centerShape) {
                radialShapes.add(shape);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- GESTION DU SCROLL (Correctif du problème de molette) ---
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Si on a sélectionné le BLOC (Centre), le scroll ne fait rien
        if (selectedShape == centerShape) {
            return false;
        }

        // Sinon, on modifie le rayon
        if (scrollY != 0) {
            // On appelle la méthode statique du client pour changer le rayon
            PiggyBuildClient.setCurrentShape(selectedShape);
            PiggyBuildClient.modifyRadius(scrollY > 0 ? 1 : -1);
            return true; // On a géré l'événement
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void tick() {
        // Vérification physique de la touche (comme vu précédemment)
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        PiggyBuildClient.setCurrentShape(selectedShape);
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        
        // --- LOGIQUE DE SÉLECTION ---
        
        // Rayon de la zone centrale (bouton "Bloc")
        double centerBtnRadius = 25.0; 

        if (dist < centerBtnRadius) {
            // Si la souris est au centre -> On sélectionne le BLOC
            selectedShape = centerShape;
        } else {
            // Sinon -> On calcule l'angle pour les parts de pizza
            double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
            if (angle < 0)
                angle += 2 * Math.PI;

            double anglePerItem = (2 * Math.PI) / radialShapes.size();
            int hoverIndex = (int) (angle / anglePerItem) % radialShapes.size();

            selectedShape = radialShapes.get(hoverIndex);
        }
        PiggyBuildClient.setCurrentShape(selectedShape);

        // --- RENDU GRAPHIQUE ---
        
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f mat = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        float outerRadius = 80f; 

        // 1. Dessin des Sections Radiales (Anneau, Ligne, Sphère)
        double anglePerItem = (2 * Math.PI) / radialShapes.size();
        
        for (int i = 0; i < radialShapes.size(); i++) {
            BuildShape shape = radialShapes.get(i);
            boolean isHovered = (shape == selectedShape);
            
            // Vert si survolé, Gris sombre sinon
            float r = isHovered ? 0.0f : 0.1f;
            float g = isHovered ? 0.8f : 0.1f;
            float b = isHovered ? 0.6f : 0.1f;
            float a = 0.6f;

            double startAngle = (i * anglePerItem) - Math.toRadians(90);
            double endAngle = ((i + 1) * anglePerItem) - Math.toRadians(90);

            // Pour ne pas couvrir le centre, on part de centerBtnRadius
            // Coin Intérieur 1
            float inX1 = (float)(centerX + Math.cos(endAngle) * centerBtnRadius);
            float inY1 = (float)(centerY + Math.sin(endAngle) * centerBtnRadius);
            // Coin Extérieur 1
            float outX1 = (float)(centerX + Math.cos(endAngle) * outerRadius);
            float outY1 = (float)(centerY + Math.sin(endAngle) * outerRadius);
            // Coin Extérieur 2
            float outX2 = (float)(centerX + Math.cos(startAngle) * outerRadius);
            float outY2 = (float)(centerY + Math.sin(startAngle) * outerRadius);
            // Coin Intérieur 2
            float inX2 = (float)(centerX + Math.cos(startAngle) * centerBtnRadius);
            float inY2 = (float)(centerY + Math.sin(startAngle) * centerBtnRadius);

            // On dessine 2 triangles pour faire un quad
            buffer.addVertex(mat, inX1, inY1, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, outX1, outY1, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, inX2, inY2, 0).setColor(r, g, b, a);

            buffer.addVertex(mat, outX1, outY1, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, outX2, outY2, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, inX2, inY2, 0).setColor(r, g, b, a);
        }

        // 2. Dessin du Centre (Bloc Unique)
        boolean centerHovered = (selectedShape == centerShape);
        float cr = centerHovered ? 0.0f : 0.2f;
        float cg = centerHovered ? 0.8f : 0.2f;
        float cb = centerHovered ? 0.6f : 0.2f;
        
        // On dessine un hexagone ou cercle au centre (approximé par des triangles)
        int segments = 16;
        for(int i=0; i<segments; i++) {
             double sAng = (i * 2 * Math.PI) / segments;
             double eAng = ((i + 1) * 2 * Math.PI) / segments;
             
             buffer.addVertex(mat, centerX, centerY, 0).setColor(cr, cg, cb, 0.8f);
             buffer.addVertex(mat, (float)(centerX + Math.cos(eAng)*centerBtnRadius), (float)(centerY + Math.sin(eAng)*centerBtnRadius), 0).setColor(cr, cg, cb, 0.8f);
             buffer.addVertex(mat, (float)(centerX + Math.cos(sAng)*centerBtnRadius), (float)(centerY + Math.sin(sAng)*centerBtnRadius), 0).setColor(cr, cg, cb, 0.8f);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // --- DESSIN DU TEXTE ---
        
        // Texte du Centre
        guiGraphics.drawCenteredString(this.font, centerShape.getDisplayName(), centerX, centerY - 4, centerHovered ? 0xFFFFFF : 0xAAAAAA);

        // Texte des Rayons
        for (int i = 0; i < radialShapes.size(); i++) {
            BuildShape shape = radialShapes.get(i);
            boolean isHovered = (shape == selectedShape);
            
            double midAngle = (i * anglePerItem + (anglePerItem / 2)) - Math.toRadians(90);
            float textRadius = outerRadius * 0.65f;
            int tx = (int) (centerX + Math.cos(midAngle) * textRadius);
            int ty = (int) (centerY + Math.sin(midAngle) * textRadius);

            guiGraphics.drawCenteredString(this.font, shape.getDisplayName(), tx, ty - 4, isHovered ? 0xFFFFFF : 0xAAAAAA);
        }
        
        RenderSystem.disableBlend();
    }
}