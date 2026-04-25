package is.pig.minecraft.build.mvc.view;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class HighlightRenderType {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("piggy-build", "textures/misc/highlight.png");
    
    public static final RenderType TYPE = is.pig.minecraft.lib.util.CompatibilityHelper.getTranslucentRenderType(TEXTURE);
}