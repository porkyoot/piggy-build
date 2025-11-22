package is.pig.minecraft.build.mvc.view;

import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class WorldShapeRenderer {

    private static final float EPSILON = 0.002f;

    public static void drawBlock(VertexConsumer builder, Matrix4f mat, int x, int y, int z, float r, float g, float b, float a) {
        for (Direction dir : Direction.values()) {
            drawFace(builder, mat, x, y, z, dir, r, g, b, a);
        }
    }

    public static void drawLine(VertexConsumer builder, Matrix4f mat, Axis axis, double radius, float r, float g, float b, float a) {
        int len = (int) Math.ceil(radius);
        for (int i = -len; i <= len; i++) {
            int x=0, y=0, z=0;
            switch(axis) { case X -> x=i; case Y -> y=i; case Z -> z=i; }
            drawBlock(builder, mat, x, y, z, r, g, b, a);
        }
    }

    /**
     * Restauration de la logique complète pour l'Anneau (Ring)
     */
    public static void drawRing(VertexConsumer builder, Matrix4f mat, Axis axis, double radius, float r, float g, float b, float a) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        // Determine walls to check based on axis
        Direction[] wallsToCheck;
        switch (axis) {
            case Y -> wallsToCheck = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            case Z -> wallsToCheck = new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
            case X -> wallsToCheck = new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            default -> { return; }
        }

        for (int u = min; u <= max; u++) {
            for (int v = min; v <= max; v++) {
                
                double currentDistSq = getDistSq2D(u, v);

                if (currentDistSq <= radiusSq && currentDistSq > innerRadiusSq) {
                    
                    // Map 2D (u,v) -> 3D (x,y,z)
                    int x=0, y=0, z=0;
                    switch (axis) {
                        case Y -> { x = u; z = v; }
                        case Z -> { x = u; y = v; }
                        case X -> { z = u; y = v; }
                    }

                    for (Direction dir : wallsToCheck) {
                        int neighborX = x + dir.getStepX();
                        int neighborY = y + dir.getStepY();
                        int neighborZ = z + dir.getStepZ();

                        // Map back 3D neighbor -> 2D (nu, nv)
                        int nu=0, nv=0;
                        switch (axis) {
                            case Y -> { nu = neighborX; nv = neighborZ; }
                            case Z -> { nu = neighborX; nv = neighborY; }
                            case X -> { nu = neighborZ; nv = neighborY; }
                        }

                        double neighborDistSq = getDistSq2D(nu, nv);

                        // Draw face if neighbor is outside the ring OR closer to center (inner wall)
                        boolean neighborIsSolid = (neighborDistSq <= radiusSq && neighborDistSq > innerRadiusSq);
                        if (!neighborIsSolid) {
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
     * Restauration de la logique complète pour la Sphère
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

                    double currentDistSq = getDistSq3D(x, y, z);

                    if (currentDistSq <= radiusSq && currentDistSq > innerRadiusSq) {
                        
                        for (Direction dir : Direction.values()) {
                            int nx = x + dir.getStepX();
                            int ny = y + dir.getStepY();
                            int nz = z + dir.getStepZ();

                            double neighborDistSq = getDistSq3D(nx, ny, nz);
                            boolean neighborIsSolid = (neighborDistSq <= radiusSq && neighborDistSq > innerRadiusSq);

                            if (!neighborIsSolid) {
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

    // --- Helpers ---

    private static double getDistSq2D(int u, int v) {
        return ((u + 0.5) * (u + 0.5)) + ((v + 0.5) * (v + 0.5));
    }

    private static double getDistSq3D(int x, int y, int z) {
        return ((x + 0.5) * (x + 0.5)) + ((y + 0.5) * (y + 0.5)) + ((z + 0.5) * (z + 0.5));
    }

    private static void drawFace(VertexConsumer builder, Matrix4f mat, int x, int y, int z, Direction face, float r, float g, float b, float a) {
        float x0 = x - EPSILON, x1 = x + 1 + EPSILON;
        float y0 = y - EPSILON, y1 = y + 1 + EPSILON;
        float z0 = z - EPSILON, z1 = z + 1 + EPSILON;

        switch (face) {
            case DOWN -> quad(builder, mat, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
            case UP -> quad(builder, mat, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
            case NORTH -> quad(builder, mat, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
            case SOUTH -> quad(builder, mat, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
            case WEST -> quad(builder, mat, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
            case EAST -> quad(builder, mat, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
        }
    }

    private static void quad(VertexConsumer b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float blue, float a) {
        b.addVertex(m, x1, y1, z1).setUv(0, 1).setColor(r, g, blue, a);
        b.addVertex(m, x2, y2, z2).setUv(1, 1).setColor(r, g, blue, a);
        b.addVertex(m, x3, y3, z3).setUv(1, 0).setColor(r, g, blue, a);
        b.addVertex(m, x4, y4, z4).setUv(0, 0).setColor(r, g, blue, a);
    }
}