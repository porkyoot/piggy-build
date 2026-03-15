package is.pig.minecraft.build.mvc.view;

import com.mojang.blaze3d.vertex.*;
import is.pig.minecraft.build.config.PiggyBuildConfig;
import is.pig.minecraft.build.mvc.controller.InputController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.awt.Color;

public class LightLevelOverlayRenderer {

    private static final ResourceLocation MONSTER_OVERLAY = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/misc/monster_overlay.png");
    private static final ResourceLocation MONSTER_OVERLAY_SIDE = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/misc/monster_overlay_side.png");

    public static RenderType getRenderType(ResourceLocation texture) {
        return RenderType.create("piggy_light_level_" + texture.getPath().replace("/", "_").replace(".", "_"),
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
    }

    public static void render(Minecraft mc, Vec3 cameraPos, PoseStack stack, MultiBufferSource.BufferSource buffers) {
        if (!InputController.getLightLevelOverlayHandler().isActive() || mc.level == null || mc.player == null) {
            return;
        }

        Color color = PiggyBuildConfig.getInstance().getLightLevelOverlayColor();
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = color.getAlpha() / 255.0f;

        BlockPos playerPos = mc.player.blockPosition();
        int radius = 16;
        int radiusY = 8;

        VertexConsumer topBuilder = buffers.getBuffer(getRenderType(MONSTER_OVERLAY));
        VertexConsumer sideBuilder = buffers.getBuffer(getRenderType(MONSTER_OVERLAY_SIDE));

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radiusY; y <= radiusY; y++) {
                    BlockPos pos = playerPos.offset(x, y, z);
                    
                    if (mc.level.getBrightness(LightLayer.BLOCK, pos) == 0) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (!state.canOcclude()) {
                            BlockPos below = pos.below();
                            BlockState belowState = mc.level.getBlockState(below);
                            
                            if (belowState.isFaceSturdy(mc.level, below, Direction.UP)) {
                                boolean n = mc.level.getBrightness(LightLayer.BLOCK, pos.north()) > 0;
                                boolean s = mc.level.getBrightness(LightLayer.BLOCK, pos.south()) > 0;
                                boolean e = mc.level.getBrightness(LightLayer.BLOCK, pos.east()) > 0;
                                boolean w = mc.level.getBrightness(LightLayer.BLOCK, pos.west()) > 0;

                                if (n || s || e || w) {
                                    Direction primaryDir = Direction.NORTH;
                                    if (n) primaryDir = Direction.NORTH;
                                    else if (s) primaryDir = Direction.SOUTH;
                                    else if (e) primaryDir = Direction.EAST;
                                    else if (w) primaryDir = Direction.WEST;

                                    renderQuad(topBuilder, stack, cameraPos, pos, primaryDir, 0.005f, r, g, b, a);
                                    
                                    if (n && primaryDir != Direction.NORTH) renderQuad(sideBuilder, stack, cameraPos, pos, Direction.NORTH, 0.010f, r, g, b, a);
                                    if (s && primaryDir != Direction.SOUTH) renderQuad(sideBuilder, stack, cameraPos, pos, Direction.SOUTH, 0.015f, r, g, b, a);
                                    if (e && primaryDir != Direction.EAST)  renderQuad(sideBuilder, stack, cameraPos, pos, Direction.EAST,  0.020f, r, g, b, a);
                                    if (w && primaryDir != Direction.WEST)  renderQuad(sideBuilder, stack, cameraPos, pos, Direction.WEST,  0.025f, r, g, b, a);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderQuad(VertexConsumer builder, PoseStack stack, Vec3 cameraPos, BlockPos pos, Direction face, float yOffset, float r, float g, float b, float a) {
        stack.pushPose();
        
        double rx = pos.getX() - cameraPos.x;
        double ry = pos.getY() - cameraPos.y;
        double rz = pos.getZ() - cameraPos.z;
        stack.translate(rx, ry, rz);

        stack.translate(0.5f, yOffset, 0.5f);
        
        switch (face) {
            case NORTH: 
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0));
                break;
            case SOUTH: 
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                break;
            case WEST: 
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));
                break;
            case EAST: 
                stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));
                break;
            default:
                break;
        }

        stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));
        
        Matrix4f mat = stack.last().pose();
        float size = 0.5f;

        builder.addVertex(mat, -size, -size, 0).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, -size, size, 0).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, size, size, 0).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, size, -size, 0).setUv(1, 0).setColor(r, g, b, a);
        
        stack.popPose();
    }
}
