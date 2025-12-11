package is.pig.minecraft.build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.config.ConfigPersistence;
import is.pig.minecraft.build.mvc.controller.InputController;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.PlacementSession;
import is.pig.minecraft.build.mvc.view.FastPlaceOverlay;
import is.pig.minecraft.build.mvc.view.DirectionalPlacementRenderer;
import is.pig.minecraft.build.mvc.view.HighlightRenderType;
import is.pig.minecraft.build.mvc.view.WorldShapeRenderer;
import is.pig.minecraft.lib.config.PiggyClientConfig;
import is.pig.minecraft.lib.network.SyncConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

public class PiggyBuildClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("piggy-build");

    private final InputController controller = new InputController();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Ehlo from Piggy Build!");

        // 0. Register features
        is.pig.minecraft.lib.features.CheatFeatureRegistry.register(
                new is.pig.minecraft.lib.features.CheatFeature(
                        "fast_place",
                        "Fast Place",
                        "Rapidly place blocks by holding right-click",
                        false));

        is.pig.minecraft.lib.features.CheatFeatureRegistry.register(
                new is.pig.minecraft.lib.features.CheatFeature(
                        "flexible_placement",
                        "Flexible Placement",
                        "Place blocks in different directions and diagonally",
                        true));

        // 1. Load configuration
        ConfigPersistence.load();

        // 2. Initialize controller
        controller.initialize();

        // 3. Register Anti-Cheat HUD Overlay
        is.pig.minecraft.lib.ui.AntiCheatHudOverlay.register();

        // 4. HUD overlay rendering
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            FastPlaceOverlay.render(graphics, tickDelta);
        });

        SyncConfigPayload.registerPacket();
        
        // Register Config Sync Listener
        is.pig.minecraft.lib.config.PiggyClientConfig.getInstance().registerConfigSyncListener((allowCheats, features) -> {
            is.pig.minecraft.build.config.PiggyBuildConfig buildConfig = is.pig.minecraft.build.config.PiggyBuildConfig.getInstance();
            buildConfig.serverAllowCheats = allowCheats;
            buildConfig.serverFeatures = features;

            // Proactively disable features if blocked.
            // We set the fields directly or use internal setters to avoid triggering the user feedback 
            // ("You cannot enable this") when the SERVER is the one disabling it.
            if (!allowCheats) {
                // If cheats are globally disabled by server, turn off features
                buildConfig.setFastPlaceEnabled(false);
                buildConfig.setFlexiblePlacementEnabled(false); 
            }

            // Also check specific feature flags
            if (features != null) {
                if (features.containsKey("fast_place") && !features.get("fast_place")) {
                    buildConfig.setFastPlaceEnabled(false);
                }
                if (features.containsKey("flexible_placement") && !features.get("flexible_placement")) {
                    buildConfig.setFlexiblePlacementEnabled(false);
                }
            }

            LOGGER.info("[ANTI-CHEAT DEBUG] PiggyBuildConfig updated from server sync: allowCheats={}, features={}", allowCheats, features);
        });

        // 4. Render loop (visualization)
        WorldRenderEvents.LAST.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            Camera camera = context.camera();
            Vec3 cameraPos = camera.getPosition();
            PoseStack stack = context.matrixStack();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

            renderBuildShapes(mc, cameraPos, stack, buffers);
            renderDirectionalPlacement(mc, cameraPos, stack, buffers);
        });
    }

    private void renderBuildShapes(Minecraft mc, Vec3 cameraPos, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        BuildSession session = BuildSession.getInstance();
        if (!session.isActive()) return;

        VertexConsumer builder = buffers.getBuffer(HighlightRenderType.TYPE);
        PiggyBuildConfig config = PiggyBuildConfig.getInstance();
        float[] rgba = config.getHighlightColor().getComponents(null);
        float r = rgba[0], g = rgba[1], b = rgba[2], a = rgba[3];

        double rx = session.getAnchorPos().getX() - cameraPos.x;
        double ry = session.getAnchorPos().getY() - cameraPos.y;
        double rz = session.getAnchorPos().getZ() - cameraPos.z;

        stack.pushPose();
        stack.translate(rx, ry, rz);

        switch (session.getShape()) {
            case BLOCK -> WorldShapeRenderer.drawBlock(builder, stack.last().pose(), 0, 0, 0, r, g, b, a);
            case LINE -> WorldShapeRenderer.drawLine(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
            case SPHERE -> WorldShapeRenderer.drawSphere(builder, stack.last().pose(), session.getRadius(), r, g, b, a);
            case RING -> WorldShapeRenderer.drawRing(builder, stack.last().pose(), session.getAnchorAxis(), session.getRadius(), r, g, b, a);
        }

        stack.popPose();
        buffers.endBatch(HighlightRenderType.TYPE);
    }

    private void renderDirectionalPlacement(Minecraft mc, Vec3 cameraPos, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        PlacementSession session = PlacementSession.getInstance();
        if (!session.isActive()) return;
        
        // Double check config state to ensure we don't render overlays if disabled
        if (!PiggyBuildConfig.getInstance().isFeatureFlexiblePlacementEnabled()) return;

        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) mc.hitResult;
            Direction offset = session.getCurrentOffset();

            net.minecraft.resources.ResourceLocation tex = (offset == null)
                    ? DirectionalPlacementRenderer.getCenterTexture()
                    : DirectionalPlacementRenderer.getArrowTexture();

            VertexConsumer overlayBuilder = buffers.getBuffer(DirectionalPlacementRenderer.getRenderType(tex));

            double rx = hit.getBlockPos().getX() - cameraPos.x;
            double ry = hit.getBlockPos().getY() - cameraPos.y;
            double rz = hit.getBlockPos().getZ() - cameraPos.z;

            stack.pushPose();
            stack.translate(rx, ry, rz);

            PiggyBuildConfig config = PiggyBuildConfig.getInstance();
            float[] placementRgba = config.getPlacementOverlayColor().getComponents(null);
            
            DirectionalPlacementRenderer.render(
                    overlayBuilder,
                    stack,
                    hit.getDirection(),
                    offset,
                    placementRgba[0], placementRgba[1], placementRgba[2], placementRgba[3]);

            stack.popPose();
            buffers.endBatch(DirectionalPlacementRenderer.getRenderType(tex));
        }
    }
}