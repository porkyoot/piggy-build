package is.pig.minecraft.build.mvc.model;

import net.minecraft.core.Direction;

public class PlacementSession {
    private static final PlacementSession INSTANCE = new PlacementSession();

    private boolean active = false;
    private Direction currentOffset = null;

    private PlacementSession() {}

    public static PlacementSession getInstance() {
        return INSTANCE;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Direction getCurrentOffset() { return currentOffset; }
    public void setCurrentOffset(Direction offset) { this.currentOffset = offset; }
}