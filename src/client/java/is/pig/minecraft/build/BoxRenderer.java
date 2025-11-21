package is.pig.minecraft.build;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class BoxRenderer {
    public static void drawBoxFill(VertexConsumer builder, Matrix4f mat, float r, float g, float b, float a) {
        // Bottom
        builder.addVertex(mat, 0, 0, 0).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 0, 0).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 0, 1).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 0, 1).setUv(0, 1).setColor(r, g, b, a);
        // Top
        builder.addVertex(mat, 0, 1, 0).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 1, 1).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 1, 1).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 1, 0).setUv(1, 1).setColor(r, g, b, a);
        // North
        builder.addVertex(mat, 0, 0, 0).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 1, 0).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 1, 0).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 0, 0).setUv(0, 1).setColor(r, g, b, a);
        // South
        builder.addVertex(mat, 0, 0, 1).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 0, 1).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 1, 1).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 1, 1).setUv(0, 0).setColor(r, g, b, a);
        // West
        builder.addVertex(mat, 0, 0, 0).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 0, 1).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 1, 1).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 0, 1, 0).setUv(0, 0).setColor(r, g, b, a);
        // East
        builder.addVertex(mat, 1, 0, 0).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 1, 0).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 1, 1).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, 1, 0, 1).setUv(0, 1).setColor(r, g, b, a);
    }
}

