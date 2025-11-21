package is.pig.minecraft.build;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class ShapeRenderer {

    /**
     * Dessine un bloc complet (les 6 faces) à la position donnée.
     * Utilise la méthode factorisée drawFace pour éviter la répétition.
     */
    public static void drawBlock(VertexConsumer builder, Matrix4f mat, int x, int y, int z, float r, float g, float b, float a) {
        // Direction.values() contient [DOWN, UP, NORTH, SOUTH, WEST, EAST]
        // On boucle simplement dessus pour dessiner tout le cube.
        for (Direction dir : Direction.values()) {
            drawFace(builder, mat, x, y, z, dir, r, g, b, a);
        }
    }

    /**
     * Dessine les BORDURES d'un anneau (effet escalier) en ignorant les faces plates.
     * Le code est entièrement factorisé pour gérer les 3 axes (X, Y, Z) sans répétition.
     */
    public static void drawRing(VertexConsumer builder, Matrix4f mat, Axis axis, double radius, float r, float g, float b, float a) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        // 1. SELECTION DES MURS A DESSINER
        // On ne sélectionne que les directions qui forment les "bords" de l'anneau.
        // On exclut volontairement les faces parallèles à l'anneau (le "couvercle").
        Direction[] wallsToCheck;

        switch (axis) {
            case Y: // Horizontal -> On check Nord, Sud, Est, Ouest (Pas Haut/Bas)
                wallsToCheck = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                break;
            case Z: // Vertical Face Z -> On check Haut, Bas, Est, Ouest (Pas Nord/Sud)
                wallsToCheck = new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
                break;
            case X: // Vertical Face X -> On check Haut, Bas, Nord, Sud (Pas Est/Ouest)
                wallsToCheck = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
                break;
            default:
                return;
        }

        // 2. BOUCLE UNIQUE (Abstraite)
        // On parcourt le plan 2D de l'anneau (u, v)
        for (int u = min; u <= max; u++) {
            for (int v = min; v <= max; v++) {
                
                // Si le point courant (u, v) est dans la matière de l'anneau...
                if (isPositionInRing(u, v, radiusSq, innerRadiusSq)) {
                    
                    // ... On mappe (u, v) vers les vraies coordonnées 3D (x, y, z) pour savoir où on est
                    int x = 0, y = 0, z = 0;
                    switch (axis) {
                        case Y: x = u; y = 0; z = v; break;
                        case Z: x = u; y = v; z = 0; break;
                        case X: x = 0; y = v; z = u; break;
                    }

                    // 3. VERIFICATION DES VOISINS
                    // Pour chaque direction de mur possible...
                    for (Direction dir : wallsToCheck) {
                        
                        // On regarde le bloc voisin dans cette direction
                        // (La classe Direction nous donne le décalage +1, -1 ou 0 automatiquement)
                        int neighborX = x + dir.getStepX();
                        int neighborY = y + dir.getStepY();
                        int neighborZ = z + dir.getStepZ();

                        // On doit re-transformer ce voisin 3D en coordonnées locales 2D (u, v)
                        // pour vérifier s'il est dans le cercle mathématique
                        int nu = 0, nv = 0;
                        switch (axis) {
                            case Y: nu = neighborX; nv = neighborZ; break;
                            case Z: nu = neighborX; nv = neighborY; break;
                            case X: nu = neighborZ; nv = neighborY; break;
                        }

                        // SI le voisin est VIDE (hors de l'anneau), ALORS on dessine le mur
                        if (!isPositionInRing(nu, nv, radiusSq, innerRadiusSq)) {
                            drawFace(builder, mat, x, y, z, dir, r, g, b, a);
                        }
                    }
                }
            }
        }
    }

    // Vérifie mathématiquement si un point 2D est dans l'anneau
    private static boolean isPositionInRing(int u, int v, double maxSq, double minSq) {
        double distSq = ((u + 0.5) * (u + 0.5)) + ((v + 0.5) * (v + 0.5));
        return distSq <= maxSq && distSq > minSq;
    }

    /**
     * Dessine une sphère creuse (coquille) en ne rendant que les faces frontières.
     * Fonctionne comme l'anneau, mais en 3D (X, Y, Z).
     */
    public static void drawSphere(VertexConsumer builder, Matrix4f mat, double radius, float r, float g, float b, float a) {
        // Épaisseur de la coque : 1 bloc par défaut
        double innerRadius = Math.max(0, radius - 1.0);
        
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        // 1. Triple boucle (X, Y, Z) pour parcourir le volume
        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {

                    // 2. Est-ce que ce bloc est dans la matière de la sphère ?
                    if (isPositionInSphere(x, y, z, radiusSq, innerRadiusSq)) {
                        
                        // 3. Vérification des 6 voisins (Haut, Bas, Nord, Sud, Est, Ouest)
                        // Direction.values() contient les 6 directions.
                        for (Direction dir : Direction.values()) {
                            
                            int neighborX = x + dir.getStepX();
                            int neighborY = y + dir.getStepY();
                            int neighborZ = z + dir.getStepZ();

                            // Si le voisin est VIDE (hors de la coque), on dessine la face
                            // Cela marche pour l'extérieur (vers le monde) ET l'intérieur (vers le creux de la sphère)
                            if (!isPositionInSphere(neighborX, neighborY, neighborZ, radiusSq, innerRadiusSq)) {
                                drawFace(builder, mat, x, y, z, dir, r, g, b, a);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Vérifie si un point 3D est dans la coque de la sphère.
     * Formule : min² < x² + y² + z² <= max²
     */
    private static boolean isPositionInSphere(int x, int y, int z, double maxSq, double minSq) {
        // On ajoute 0.5 pour mesurer depuis le centre du bloc
        double distSq = ((x + 0.5) * (x + 0.5)) + 
                        ((y + 0.5) * (y + 0.5)) + 
                        ((z + 0.5) * (z + 0.5));
        
        return distSq <= maxSq && distSq > minSq;
    }

    /**
     * Méthode générique pour dessiner une face.
     * Utilise l'algèbre vectorielle pour placer les points sans "if/else" complexes.
     */
    private static void drawFace(VertexConsumer builder, Matrix4f mat, int x, int y, int z, Direction face, float r, float g, float b, float a) {
        // Pour dessiner un carré, on part du coin (x, y, z)
        // Mais il faut savoir quels coins utiliser selon la direction.
        // Voici une astuce simple : on définit les 4 coins possibles d'un bloc
        float x0 = x, x1 = x + 1;
        float y0 = y, y1 = y + 1;
        float z0 = z, z1 = z + 1;

        // Selon la face, on relie les points dans le sens inverse des aiguilles d'une montre (CCW)
        // C'est la partie "formule" : on hardcode les combinaisons logiques standards d'un cube.
        
        switch (face) {
            case DOWN:  // Y-
                quad(builder, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a); break;
            case UP:    // Y+
                quad(builder, mat, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a); break;
            case NORTH: // Z-
                quad(builder, mat, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a); break;
            case SOUTH: // Z+
                quad(builder, mat, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a); break;
            case WEST:  // X-
                quad(builder, mat, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a); break;
            case EAST:  // X+
                quad(builder, mat, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a); break;
        }
    }

    // Helper final pour écrire les 4 vertices
    private static void quad(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float blue, float a) {
        // On ajoute les UVs standards (0,0 -> 1,1) pour que la texture s'applique proprement sur chaque face
        // Ordre UV : (0,1), (1,1), (1,0), (0,0) ou variantes selon rotation, 
        // ici simplifié pour une texture unie/bruitée.
        b.addVertex(m, x1, y1, z1).setUv(0, 1).setColor(r, g, blue, a);
        b.addVertex(m, x2, y2, z2).setUv(1, 1).setColor(r, g, blue, a);
        b.addVertex(m, x3, y3, z3).setUv(1, 0).setColor(r, g, blue, a);
        b.addVertex(m, x4, y4, z4).setUv(0, 0).setColor(r, g, blue, a);
    }
}