package is.pig.minecraft.build;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class ShapeRenderer {

    // Le petit décalage pour éviter le Z-fighting (scintillement)
    private static final float EPSILON = 0.002f;

    /**
     * Dessine l'anneau en ne gardant QUE le bord extérieur.
     * Les murs intérieurs (le trou) sont ignorés.
     */
    public static void drawRing(VertexConsumer builder, Matrix4f mat, Axis axis, double radius, float r, float g, float b, float a) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        // Sélection des murs selon l'axe (idem qu'avant)
        Direction[] wallsToCheck;
        switch (axis) {
            case Y: wallsToCheck = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}; break;
            case Z: wallsToCheck = new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}; break;
            case X: wallsToCheck = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH}; break;
            default: return;
        }

        for (int u = min; u <= max; u++) {
            for (int v = min; v <= max; v++) {
                
                // Distance du bloc ACTUEL par rapport au centre
                double currentDistSq = getDistSq(u, v);

                if (currentDistSq <= radiusSq && currentDistSq > innerRadiusSq) {
                    
                    // Mapping 2D -> 3D
                    int x = 0, y = 0, z = 0;
                    switch (axis) {
                        case Y: x = u; y = 0; z = v; break;
                        case Z: x = u; y = v; z = 0; break;
                        case X: x = 0; y = v; z = u; break;
                    }

                    for (Direction dir : wallsToCheck) {
                        int neighborX = x + dir.getStepX();
                        int neighborY = y + dir.getStepY();
                        int neighborZ = z + dir.getStepZ();

                        // Mapping inverse pour vérifier le voisin dans le plan 2D
                        int nu = 0, nv = 0;
                        switch (axis) {
                            case Y: nu = neighborX; nv = neighborZ; break;
                            case Z: nu = neighborX; nv = neighborY; break;
                            case X: nu = neighborZ; nv = neighborY; break;
                        }

                        // Distance du VOISIN
                        double neighborDistSq = getDistSq(nu, nv);

                        // CONDITION MAGIQUE :
                        // 1. Le voisin n'est pas dans l'anneau (c'est du vide)
                        // 2. ET le voisin est PLUS LOIN du centre que nous.
                        //    -> Si le voisin est plus près, c'est qu'on regarde vers l'intérieur (le trou), donc on ne dessine pas.
                        if (!isPositionInRing(neighborDistSq, radiusSq, innerRadiusSq)) {
                             if (neighborDistSq > currentDistSq) {
                                 drawFace(builder, mat, x, y, z, dir, r, g, b, a);
                             }
                        }
                    }
                }
            }
        }
    }

    /**
     * Dessine la sphère en ne gardant QUE la coque extérieure.
     * L'intérieur de la coquille est invisible.
     */
    public static void drawSphere(VertexConsumer builder, Matrix4f mat, double radius, float r, float g, float b, float a) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {

                    // Distance du bloc ACTUEL
                    double currentDistSq = getDistSq3D(x, y, z);

                    if (currentDistSq <= radiusSq && currentDistSq > innerRadiusSq) {
                        
                        for (Direction dir : Direction.values()) {
                            int nx = x + dir.getStepX();
                            int ny = y + dir.getStepY();
                            int nz = z + dir.getStepZ();

                            // Distance du VOISIN
                            double neighborDistSq = getDistSq3D(nx, ny, nz);

                            // MEME LOGIQUE : On ne dessine que si le voisin est "vers l'extérieur"
                            if (!isPositionInSphere(neighborDistSq, radiusSq, innerRadiusSq)) {
                                if (neighborDistSq > currentDistSq) {
                                    drawFace(builder, mat, x, y, z, dir, r, g, b, a);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- HELPERS ---

    private static double getDistSq(int u, int v) {
        return ((u + 0.5) * (u + 0.5)) + ((v + 0.5) * (v + 0.5));
    }

    private static double getDistSq3D(int x, int y, int z) {
        return ((x + 0.5) * (x + 0.5)) + ((y + 0.5) * (y + 0.5)) + ((z + 0.5) * (z + 0.5));
    }

    private static boolean isPositionInRing(double distSq, double maxSq, double minSq) {
        return distSq <= maxSq && distSq > minSq;
    }
    
    private static boolean isPositionInSphere(double distSq, double maxSq, double minSq) {
        return distSq <= maxSq && distSq > minSq;
    }

    /**
     * Dessine une ligne de blocs le long de l'axe.
     * Longueur = Radius * 2 (de -radius à +radius)
     */
    public static void drawLine(VertexConsumer builder, Matrix4f mat, Axis axis, double radius, float r, float g, float b, float a) {
        int len = (int) Math.ceil(radius);

        for (int i = -len; i <= len; i++) {
            // Coordonnées selon l'axe
            int x = 0, y = 0, z = 0;
            switch (axis) {
                case X: x = i; break;
                case Y: y = i; break;
                case Z: z = i; break;
            }
            
            // On dessine le bloc complet (comme pour BLOCK)
            drawBlock(builder, mat, x, y, z, r, g, b, a);
        }
    }

    // --- DESSIN DES FACES (Inchangé) ---

    public static void drawBlock(VertexConsumer builder, Matrix4f mat, int x, int y, int z, float r, float g, float b, float a) {
        for (Direction dir : Direction.values()) {
            drawFace(builder, mat, x, y, z, dir, r, g, b, a);
        }
    }

    private static void drawFace(VertexConsumer builder, Matrix4f mat, int x, int y, int z, Direction face, float r, float g, float b, float a) {
        // On définit les coins du bloc, mais LÉGÈREMENT AGRANDIS
        float x0 = x - EPSILON, x1 = x + 1 + EPSILON;
        float y0 = y - EPSILON, y1 = y + 1 + EPSILON;
        float z0 = z - EPSILON, z1 = z + 1 + EPSILON;

        switch (face) {
            case DOWN:  quad(builder, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a); break;
            case UP:    quad(builder, mat, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a); break;
            case NORTH: quad(builder, mat, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a); break;
            case SOUTH: quad(builder, mat, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a); break;
            case WEST:  quad(builder, mat, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a); break;
            case EAST:  quad(builder, mat, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a); break;
        }
    }

    private static void quad(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float blue, float a) {
        b.addVertex(m, x1, y1, z1).setUv(0, 1).setColor(r, g, blue, a);
        b.addVertex(m, x2, y2, z2).setUv(1, 1).setColor(r, g, blue, a);
        b.addVertex(m, x3, y3, z3).setUv(1, 0).setColor(r, g, blue, a);
        b.addVertex(m, x4, y4, z4).setUv(0, 0).setColor(r, g, blue, a);
    }
}