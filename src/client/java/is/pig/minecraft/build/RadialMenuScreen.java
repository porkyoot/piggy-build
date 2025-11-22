package is.pig.minecraft.build;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
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

    // --- CONSTANTES DE TAILLE (Prend en compte le GUI Scale automatiquement) ---
    private static final int ICON_SIZE = 32;
    private static final float MENU_INNER_RADIUS = 30f; // Rayon intérieur de l'anneau (trou central)
    private static final float MENU_OUTER_RADIUS = 80f; // Rayon extérieur de l'anneau
    private static final float ICON_DISTANCE = 55f;     // Distance des icônes par rapport au centre
    private static final float CENTER_BUTTON_SIZE = 40f; // Taille de la zone centrale (Bloc)

    private BuildShape selectedShape;
    private final BuildShape centerShape = BuildShape.BLOCK;
    private final List<BuildShape> radialShapes = new ArrayList<>();

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
        // Fond standard grisé
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Centre de l'écran (Scale déjà pris en compte par Screen.width/height)
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        
        // --- LOGIQUE DE DETECTION ---
        if (dist < MENU_INNER_RADIUS) {
            selectedShape = centerShape;
        } else {
            double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
            if (angle < 0) angle += 2 * Math.PI;

            double anglePerItem = (2 * Math.PI) / radialShapes.size();
            int hoverIndex = (int) (angle / anglePerItem) % radialShapes.size();
            
            selectedShape = radialShapes.get(hoverIndex);
        }

        // Mise à jour Live
        if (PiggyBuildClient.getShape() != selectedShape) {
            PiggyBuildClient.setShape(selectedShape);
        }

        // --- RENDU DES ARCS (Le Fond du Menu) ---
        
        PoseStack poseStack = guiGraphics.pose();
        Matrix4f mat = poseStack.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        double anglePerItem = (2 * Math.PI) / radialShapes.size();
        
        // 1. Dessin des secteurs radiaux
        for (int i = 0; i < radialShapes.size(); i++) {
            BuildShape shape = radialShapes.get(i);
            boolean isHovered = (shape == selectedShape);
            
            // Couleur par défaut (Blanc transparent) vs Couleur de surbrillance
            float r, g, b, a;
            
            if (isHovered) {
                // On utilise la couleur de la config !
                r = PiggyBuildClient.HIGHLIGHT_RED;
                g = PiggyBuildClient.HIGHLIGHT_GREEN;
                b = PiggyBuildClient.HIGHLIGHT_BLUE;
                a = 0.6f; // Un peu plus opaque
            } else {
                // Blanc/Gris transparent par défaut
                r = 1.0f; g = 1.0f; b = 1.0f; a = 0.2f; 
            }

            double startAngle = (i * anglePerItem) - Math.toRadians(90);
            double endAngle = ((i + 1) * anglePerItem) - Math.toRadians(90);

            // On laisse un petit espace (gap) entre les arcs pour le style
            double gap = Math.toRadians(2); 
            
            drawPieArc(buffer, mat, centerX, centerY, MENU_INNER_RADIUS + 2, MENU_OUTER_RADIUS, startAngle + gap, endAngle - gap, r, g, b, a);
        }
        
        // 2. Dessin du Cercle Central (Bloc)
        boolean isCenterHovered = (selectedShape == centerShape);
        float cr, cg, cb, ca;
        if (isCenterHovered) {
             cr = PiggyBuildClient.HIGHLIGHT_RED;
             cg = PiggyBuildClient.HIGHLIGHT_GREEN;
             cb = PiggyBuildClient.HIGHLIGHT_BLUE;
             ca = 0.6f;
        } else {
             cr = 1.0f; cg = 1.0f; cb = 1.0f; ca = 0.2f;
        }
        
        // Dessin du centre (un simple cercle plein)
        drawPieArc(buffer, mat, centerX, centerY, 0, MENU_INNER_RADIUS - 2, 0, Math.PI * 2, cr, cg, cb, ca);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();


        // --- RENDU DES ICONES ---
        
        // 1. Icones Radiales
        for (int i = 0; i < radialShapes.size(); i++) {
            BuildShape shape = radialShapes.get(i);
            boolean isSelected = (shape == selectedShape);
            
            double midAngle = (i * anglePerItem + (anglePerItem / 2)) - Math.toRadians(90);
            
            int iconX = (int) (centerX + Math.cos(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);
            int iconY = (int) (centerY + Math.sin(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);

            // Dessin de l'icône avec Color Tint si sélectionné
            drawTintedIcon(guiGraphics, shape, iconX, iconY, isSelected);

            // Rayon à côté de l'icône
            if (isSelected) {
                drawRadiusValue(guiGraphics, iconX, iconY, midAngle);
            }
        }

        // 2. Icone Centrale
        int centerIconX = centerX - (ICON_SIZE / 2);
        int centerIconY = centerY - (ICON_SIZE / 2);
        drawTintedIcon(guiGraphics, centerShape, centerIconX, centerIconY, isCenterHovered);
    }

    /**
     * Dessine un arc de cercle (secteur de donut).
     */
    private void drawPieArc(VertexConsumer buffer, Matrix4f mat, float cx, float cy, float innerR, float outerR, double startAngle, double endAngle, float r, float g, float b, float a) {
        int segments = 16; // Précision de l'arc
        double step = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            double ang1 = startAngle + (i * step);
            double ang2 = startAngle + ((i + 1) * step);

            float xIn1 = (float) (cx + Math.cos(ang1) * innerR);
            float yIn1 = (float) (cy + Math.sin(ang1) * innerR);
            float xOut1 = (float) (cx + Math.cos(ang1) * outerR);
            float yOut1 = (float) (cy + Math.sin(ang1) * outerR);

            float xIn2 = (float) (cx + Math.cos(ang2) * innerR);
            float yIn2 = (float) (cy + Math.sin(ang2) * innerR);
            float xOut2 = (float) (cx + Math.cos(ang2) * outerR);
            float yOut2 = (float) (cy + Math.sin(ang2) * outerR);

            // 2 Triangles pour faire un segment de donut
            // T1
            buffer.addVertex(mat, xIn1, yIn1, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, xOut1, yOut1, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, xIn2, yIn2, 0).setColor(r, g, b, a);
            
            // T2
            buffer.addVertex(mat, xOut1, yOut1, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, xOut2, yOut2, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, xIn2, yIn2, 0).setColor(r, g, b, a);
        }
    }

    /**
     * Dessine l'icône. Si hovered, on applique un filtre de couleur (la couleur de config).
     */
    private void drawTintedIcon(GuiGraphics guiGraphics, BuildShape shape, int x, int y, boolean isSelected) {
        RenderSystem.enableBlend();
        
        if (isSelected) {
            // On teinte l'image avec la couleur de config (donne un aspect brillant/coloré)
            // Attention : setShaderColor multiplie la couleur de la texture.
            // Si l'icône est blanche, elle deviendra Teal.
            RenderSystem.setShaderColor(
                PiggyBuildClient.HIGHLIGHT_RED, 
                PiggyBuildClient.HIGHLIGHT_GREEN, 
                PiggyBuildClient.HIGHLIGHT_BLUE, 
                1.0f);
        } else {
            // Couleur normale (Blanc = pas de changement)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        guiGraphics.blit(shape.getIcon(), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        
        // Important : Reset de la couleur pour ne pas affecter le texte ou les autres éléments
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void drawRadiusValue(GuiGraphics guiGraphics, int iconX, int iconY, double angleRad) {
        String radiusText = String.valueOf((int)PiggyBuildClient.getCurrentRadius());
        // On décale le texte un peu plus loin à l'extérieur
        float textDistance = ICON_SIZE * 0.8f; 
        
        int textX = iconX + (ICON_SIZE / 2) + (int)(Math.cos(angleRad) * textDistance);
        int textY = iconY + (ICON_SIZE / 2) + (int)(Math.sin(angleRad) * textDistance);

        int textWidth = this.font.width(radiusText);
        textX -= textWidth / 2;
        textY -= this.font.lineHeight / 2;

        guiGraphics.drawString(this.font, radiusText, textX, textY, 0xFFFFFF, true);
    }
}