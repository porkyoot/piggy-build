package is.pig.minecraft.build.api;

public interface I2DRenderer {
    void drawText(String text, int x, int y, int color);
    void drawRect(int x, int y, int width, int height, int color);
    void drawTexture(String resourcePath, int x, int y, int width, int height);
}
