package is.pig.minecraft.build.mvc.model;

/**
 * Pure Java model for build session.
 */
public class BuildSession {
    private static final BuildSession INSTANCE = new BuildSession();

    private BuildShape currentShape = BuildShape.RING;
    private double radius = 4.0;
    private int anchorX;
    private int anchorY;
    private int anchorZ;
    private String anchorAxis = "Y";
    private boolean active = false;

    private BuildSession() {}

    public static BuildSession getInstance() {
        return INSTANCE;
    }

    public BuildShape getShape() { return currentShape; }
    public void setShape(BuildShape shape) { this.currentShape = shape; }

    public double getRadius() { return radius; }
    public void modifyRadius(double delta) {
        this.radius += delta;
        if (this.radius < 1) this.radius = 1;
        if (this.radius > 64) this.radius = 64;
    }

    public int getAnchorX() { return anchorX; }
    public int getAnchorY() { return anchorY; }
    public int getAnchorZ() { return anchorZ; }
    public String getAnchorAxis() { return anchorAxis; }
    
    public void setAnchor(int x, int y, int z, String axis) {
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.anchorAxis = axis;
        this.active = true;
    }
    
    public void clearAnchor() {
        this.active = false;
    }
    
    public boolean isActive() {
        return active;
    }
}
