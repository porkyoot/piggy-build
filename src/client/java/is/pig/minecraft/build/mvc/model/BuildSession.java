package is.pig.minecraft.build.mvc.model;
import is.pig.minecraft.api.*;

public class BuildSession {
    
    private static final BuildSession INSTANCE = new BuildSession();

    private BuildShape currentShape = BuildShape.RING;
    private double radius = 4.0;
    private BlockPos anchorPos = null;
    private Direction.Axis anchorAxis = Direction.Axis.Y;

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

    public BlockPos getAnchorPos() { return anchorPos; }
    public Direction.Axis getAnchorAxis() { return anchorAxis; }
    
    public void setAnchor(BlockPos pos, Direction.Axis axis) {
        this.anchorPos = pos;
        this.anchorAxis = axis;
    }
    
    public void clearAnchor() {
        this.anchorPos = null;
    }
    
    public boolean isActive() {
        return anchorPos != null;
    }
}