package is.pig.minecraft.build.api;

/**
 * Pure Java interface for drawing shapes without PoseStack or VertexConsumer.
 */
public interface IRenderOverlay {
    void drawLine(IVector3 start, IVector3 end, float r, float g, float b, float a);
    void drawBox(IVector3 min, IVector3 max, float r, float g, float b, float a);
    void drawSphere(IVector3 center, double radius, float r, float g, float b, float a);
}
