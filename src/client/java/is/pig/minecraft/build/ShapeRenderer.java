package is.pig.minecraft.build;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.Direction;

public class ShapeRenderer {

/**
     * Dessine un anneau orienté selon un Axe (X, Y ou Z).
     * @param axis L'axe perpendiculaire à l'anneau (Y = Horizontal, X/Z = Vertical)
     */
    public static void drawRing(VertexConsumer builder, Matrix4f mat, Direction.Axis axis, double radius, float r, float g, float b, float a) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        // Selon l'axe, on change quelles coordonnées on boucle (u, v) et quelle coordonnée reste fixe (depth)
        
        // --- CAS 1 : HORIZONTAL (Axe Y) ---
        if (axis == Direction.Axis.Y) {
            for (int x = min; x <= max; x++) {
                for (int z = min; z <= max; z++) {
                    if (isPositionInRing(x, z, radiusSq, innerRadiusSq)) {
                        // Faces plates (Dessus/Dessous)
                        addQuadTop(builder, mat, x, 0, z, r, g, b, a);
                        addQuadBottom(builder, mat, x, 0, z, r, g, b, a);

                        // Bords (Escalier)
                        if (!isPositionInRing(x - 1, z, radiusSq, innerRadiusSq)) addQuadWest(builder, mat, x, 0, z, r, g, b, a);
                        if (!isPositionInRing(x + 1, z, radiusSq, innerRadiusSq)) addQuadEast(builder, mat, x, 0, z, r, g, b, a);
                        if (!isPositionInRing(x, z - 1, radiusSq, innerRadiusSq)) addQuadNorth(builder, mat, x, 0, z, r, g, b, a);
                        if (!isPositionInRing(x, z + 1, radiusSq, innerRadiusSq)) addQuadSouth(builder, mat, x, 0, z, r, g, b, a);
                    }
                }
            }
        }
        
        // --- CAS 2 : VERTICAL FACE NORD/SUD (Axe Z) ---
        // On boucle sur X et Y. Z est fixe.
        else if (axis == Direction.Axis.Z) {
            for (int x = min; x <= max; x++) {
                for (int y = min; y <= max; y++) {
                    if (isPositionInRing(x, y, radiusSq, innerRadiusSq)) {
                        // Faces plates (Nord/Sud) -> Ce sont les faces principales de l'anneau
                        addQuadNorth(builder, mat, x, y, 0, r, g, b, a);
                        addQuadSouth(builder, mat, x, y, 0, r, g, b, a);

                        // Bords (Escalier)
                        if (!isPositionInRing(x - 1, y, radiusSq, innerRadiusSq)) addQuadWest(builder, mat, x, y, 0, r, g, b, a);
                        if (!isPositionInRing(x + 1, y, radiusSq, innerRadiusSq)) addQuadEast(builder, mat, x, y, 0, r, g, b, a);
                        if (!isPositionInRing(x, y - 1, radiusSq, innerRadiusSq)) addQuadBottom(builder, mat, x, y, 0, r, g, b, a);
                        if (!isPositionInRing(x, y + 1, radiusSq, innerRadiusSq)) addQuadTop(builder, mat, x, y, 0, r, g, b, a);
                    }
                }
            }
        }

        // --- CAS 3 : VERTICAL FACE EST/OUEST (Axe X) ---
        // On boucle sur Z et Y. X est fixe.
        else if (axis == Direction.Axis.X) {
            for (int z = min; z <= max; z++) {
                for (int y = min; y <= max; y++) {
                    // Attention : ici 'z' joue le rôle de la coordonnée horizontale et 'y' verticale
                    if (isPositionInRing(z, y, radiusSq, innerRadiusSq)) {
                        // Faces plates (Est/Ouest)
                        addQuadWest(builder, mat, 0, y, z, r, g, b, a);
                        addQuadEast(builder, mat, 0, y, z, r, g, b, a);

                        // Bords (Escalier)
                        // On vérifie les voisins en Z et Y
                        if (!isPositionInRing(z - 1, y, radiusSq, innerRadiusSq)) addQuadNorth(builder, mat, 0, y, z, r, g, b, a);
                        if (!isPositionInRing(z + 1, y, radiusSq, innerRadiusSq)) addQuadSouth(builder, mat, 0, y, z, r, g, b, a);
                        if (!isPositionInRing(z, y - 1, radiusSq, innerRadiusSq)) addQuadBottom(builder, mat, 0, y, z, r, g, b, a);
                        if (!isPositionInRing(z, y + 1, radiusSq, innerRadiusSq)) addQuadTop(builder, mat, 0, y, z, r, g, b, a);
                    }
                }
            }
        }
    }

    private static boolean isPositionInRing(int u, int v, double maxSq, double minSq) {
        double distSq = ((u + 0.5) * (u + 0.5)) + ((v + 0.5) * (v + 0.5));
        return distSq <= maxSq && distSq > minSq;
    }

    // --- Les méthodes addQuad... restent IDENTIQUES à ton code précédent ---
    // Je ne les remets pas ici pour ne pas polluer, garde celles que je t'ai données juste avant.
    // (addQuadTop, addQuadBottom, addQuadNorth, etc.)
    
    private static void addQuadTop(VertexConsumer builder, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(mat, x, y + 1, z).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x, y + 1, z + 1).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y + 1, z + 1).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y + 1, z).setUv(1, 1).setColor(r, g, b, a);
    }
    private static void addQuadBottom(VertexConsumer builder, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(mat, x, y, z).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y, z).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y, z + 1).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x, y, z + 1).setUv(0, 1).setColor(r, g, b, a);
    }
    private static void addQuadNorth(VertexConsumer builder, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(mat, x, y, z).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x, y + 1, z).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y + 1, z).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y, z).setUv(0, 1).setColor(r, g, b, a);
    }
    private static void addQuadSouth(VertexConsumer builder, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(mat, x, y, z + 1).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y, z + 1).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y + 1, z + 1).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x, y + 1, z + 1).setUv(0, 0).setColor(r, g, b, a);
    }
    private static void addQuadWest(VertexConsumer builder, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(mat, x, y, z).setUv(0, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x, y, z + 1).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x, y + 1, z + 1).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x, y + 1, z).setUv(0, 0).setColor(r, g, b, a);
    }
    private static void addQuadEast(VertexConsumer builder, Matrix4f mat, float x, float y, float z, float r, float g, float b, float a) {
        builder.addVertex(mat, x + 1, y, z).setUv(1, 1).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y + 1, z).setUv(1, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y + 1, z + 1).setUv(0, 0).setColor(r, g, b, a);
        builder.addVertex(mat, x + 1, y, z + 1).setUv(0, 1).setColor(r, g, b, a);
    }
    

    public static void drawBlock(VertexConsumer builder, Matrix4f mat, float r, float g, float b, float a) {
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

