package is.pig.minecraft.build;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Central place to create and expose RenderTypes used by the mod.
 */
public final class HighlightRenderTypes {

    public static final ResourceLocation HIGHLIGHT_TEXTURE = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/misc/highlight.png");

    public static final RenderType HIGHLIGHT_TYPE = RenderType.create(
        "piggy_highlight_textured",
        DefaultVertexFormat.POSITION_TEX_COLOR,
        VertexFormat.Mode.QUADS,
        256,
        false,
        true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(new RenderStateShard.TextureStateShard(HIGHLIGHT_TEXTURE, false, false))
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST) // Wallhack on
            .setCullState(RenderStateShard.NO_CULL)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .createCompositeState(false)
    );

    private HighlightRenderTypes() {}
}