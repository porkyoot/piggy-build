package is.pig.minecraft.build.common.view;

import is.pig.minecraft.build.api.I3DRenderer;

public class WorldShapeRenderer {

    public static void drawBlock(I3DRenderer renderer, int x, int y, int z, int color) {
        renderer.highlightBlock(x, y, z, color);
    }

    public static void drawLine(I3DRenderer renderer, String axis, double radius, int color) {
        int len = (int) Math.ceil(radius);
        for (int i = -len; i <= len; i++) {
            int x = 0, y = 0, z = 0;
            switch (axis.toUpperCase()) {
                case "X" -> x = i;
                case "Y" -> y = i;
                case "Z" -> z = i;
            }
            renderer.highlightBlock(x, y, z, color);
        }
    }

    public static void drawRing(I3DRenderer renderer, String axis, double radius, int color) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int u = min; u <= max; u++) {
            for (int v = min; v <= max; v++) {
                double currentDistSq = ((double) u * u) + ((double) v * v);
                if (currentDistSq <= radiusSq && currentDistSq > innerRadiusSq) {
                    int x = 0, y = 0, z = 0;
                    switch (axis.toUpperCase()) {
                        case "Y" -> { x = u; z = v; }
                        case "Z" -> { x = u; y = v; }
                        case "X" -> { z = u; y = v; }
                    }
                    renderer.highlightBlock(x, y, z, color);
                }
            }
        }
    }

    public static void drawSphere(I3DRenderer renderer, double radius, int color) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;

        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int x = min; x <= max; x++) {
            for (int y = min; y <= max; y++) {
                for (int z = min; z <= max; z++) {
                    double currentDistSq = ((double) x * x) + ((double) y * y) + ((double) z * z);
                    if (currentDistSq <= radiusSq && currentDistSq > innerRadiusSq) {
                        renderer.highlightBlock(x, y, z, color);
                    }
                }
            }
        }
    }
}
