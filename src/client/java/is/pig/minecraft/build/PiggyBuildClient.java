package is.pig.minecraft.build;

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

    // Le contrôleur gère les entrées (Clavier, Souris, Tick)
    private final InputController controller = new InputController();

    @Override
    public void onInitializeClient() {
        // 1. Chargement de la configuration
        PiggyConfig.load();

        // 2. Initialisation du contrôleur
        controller.initialize();

        // 3. Boucle de Rendu (Visualisation)
        WorldRenderEvents.LAST.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack stack = context.matrixStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

            // --- PARTIE 1 : VISUALISATION DES FORMES (Cercle, Sphère...) ---
            renderBuildShapes(mc, cameraPos, stack, buffers);

            // --- PARTIE 2 : PLACEMENT FLEXIBLE (Overlay sur face) ---
            renderFlexiblePlacement(mc, cameraPos, stack, buffers);
        });
    }

    /**
     * Gère le rendu de la forme figée (BuildSession).
     */
    private void renderBuildShapes(Minecraft mc, Vec3 cameraPos, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        BuildSession session = BuildSession.getInstance();
        
        // Si aucune position n'est figée, on ne dessine pas de forme
        if (!session.isActive()) return;

        VertexConsumer builder = buffers.getBuffer(HighlightRenderType.TYPE);

        // Récupération des couleurs depuis la config
        PiggyConfig config = PiggyConfig.getInstance();
        float r = config.getRedFloat();
        float g = config.getGreenFloat();
        float b = config.getBlueFloat();
        float a = config.getAlphaFloat();

        // Calcul de la position relative à la caméra
        double rx = session.getAnchorPos().getX() - cameraPos.x;
        double ry = session.getAnchorPos().getY() - cameraPos.y;
        double rz = session.getAnchorPos().getZ() - cameraPos.z;

        stack.pushPose();
        stack.translate(rx, ry, rz);

        // Délégation au renderer géométrique
        switch (session.getShape()) {
            case BLOCK -> WorldShapeRenderer.drawBlock(builder, stack.last().pose(), 0, 0, 0, r, g, b, a);
            case LINE -> WorldShapeRenderer.drawLine(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
            case SPHERE -> WorldShapeRenderer.drawSphere(builder, stack.last().pose(), session.getRadius(), r, g, b, a);
            case RING -> WorldShapeRenderer.drawRing(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
        }

        stack.popPose();
        // Envoi du batch pour la transparence
        buffers.endBatch(HighlightRenderType.TYPE);
    }

    /**
     * Gère le rendu de l'overlay de placement (PlacementSession).
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

            FlexiblePlacementRenderer.render(
                overlayBuilder, 
                stack, 
                hit.getDirection(), 
                offset // Can be null
            );

            stack.popPose();
            
            // End specific batch
            buffers.endBatch(FlexiblePlacementRenderer.getRenderType(tex));
        }
    }
}