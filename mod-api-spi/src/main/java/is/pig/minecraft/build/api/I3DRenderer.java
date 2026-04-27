package is.pig.minecraft.build.api;

public interface I3DRenderer {
    void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color);
    void drawBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color);
    void highlightBlock(int x, int y, int z, int color);
}
