package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.api.IWorldInteraction;
import is.pig.minecraft.build.mvc.model.BuildSession;
import is.pig.minecraft.build.mvc.model.BuildShape;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Coordinate generator and placer.
 * Pure Java, zero Minecraft dependencies.
 */
public class ShapePlacementHandler {

    public static void placeShape(IWorldInteraction world, BuildSession session, String itemId) {
        if (!session.isActive()) return;

        BuildShape shape = session.getShape();
        double radius = session.getRadius();
        int anchorX = session.getAnchorX();
        int anchorY = session.getAnchorY();
        int anchorZ = session.getAnchorZ();
        String axis = session.getAnchorAxis();

        List<Coordinate> coords = new ArrayList<>();

        if (shape == BuildShape.LINE) {
            generateLine(anchorX, anchorY, anchorZ, axis, radius, coords);
        } else if (shape == BuildShape.RING) {
            generateRing(anchorX, anchorY, anchorZ, axis, radius, coords);
        } else if (shape == BuildShape.SPHERE) {
            generateSphere(anchorX, anchorY, anchorZ, radius, coords);
        }

        CompletableFuture.runAsync(() -> {
            for (Coordinate c : coords) {
                world.placeBlock(c.x(), c.y(), c.z(), itemId);
                try {
                    Thread.sleep(50); // Yield/Rate-limit
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private static void generateLine(int x, int y, int z, String axis, double radius, List<Coordinate> coords) {
        int len = (int) Math.ceil(radius);
        for (int i = -len; i <= len; i++) {
            if (i == 0) continue;
            switch (axis) {
                case "X" -> coords.add(new Coordinate(x + i, y, z));
                case "Y" -> coords.add(new Coordinate(x, y + i, z));
                case "Z" -> coords.add(new Coordinate(x, y, z + i));
            }
        }
    }

    private static void generateRing(int x, int y, int z, String axis, double radius, List<Coordinate> coords) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;
        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int u = min; u <= max; u++) {
            for (int v = min; v <= max; v++) {
                double distSq = ((double) u * u) + ((double) v * v);
                if (distSq <= radiusSq && distSq > innerRadiusSq) {
                    switch (axis) {
                        case "Y" -> coords.add(new Coordinate(x + u, y, z + v));
                        case "Z" -> coords.add(new Coordinate(x + u, y + v, z));
                        case "X" -> coords.add(new Coordinate(x, y + v, z + u));
                    }
                }
            }
        }
    }

    private static void generateSphere(int x, int y, int z, double radius, List<Coordinate> coords) {
        double innerRadius = Math.max(0, radius - 1.0);
        double radiusSq = radius * radius;
        double innerRadiusSq = innerRadius * innerRadius;
        int min = (int) Math.floor(-radius);
        int max = (int) Math.ceil(radius);

        for (int cx = min; cx <= max; cx++) {
            for (int cy = min; cy <= max; cy++) {
                for (int cz = min; cz <= max; cz++) {
                    double distSq = ((double) cx * cx) + ((double) cy * cy) + ((double) cz * cz);
                    if (distSq <= radiusSq && distSq > innerRadiusSq) {
                        coords.add(new Coordinate(x + cx, y + cy, z + cz));
                    }
                }
            }
        }
    }

    public record Coordinate(int x, int y, int z) {}
}
