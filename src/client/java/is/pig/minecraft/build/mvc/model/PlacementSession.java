package is.pig.minecraft.build.mvc.model;

import is.pig.minecraft.build.lib.placement.PlacementMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class PlacementSession {
    private static final PlacementSession INSTANCE = new PlacementSession();

    private boolean active = false;
    private Direction currentOffset = null;

    // Placement locking for consistent line/stair placement
    private boolean locked = false;
    private Direction lockedOffset = null;
    private Direction lockedFace = null; // Lock the clicked face too
    private PlacementMode lockedMode = null;
    private BlockPos lastPlacedPos = null; // Track last placed block for sequential placement

    private PlacementSession() {
    }

    public static PlacementSession getInstance() {
        return INSTANCE;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Direction getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(Direction offset) {
        this.currentOffset = offset;
    }

    // Locking methods
    public boolean isLocked() {
        return locked;
    }

    public Direction getLockedOffset() {
        return lockedOffset;
    }

    public Direction getLockedFace() {
        return lockedFace;
    }

    public PlacementMode getLockedMode() {
        return lockedMode;
    }

    public BlockPos getLastPlacedPos() {
        return lastPlacedPos;
    }

    public void setLastPlacedPos(BlockPos pos) {
        this.lastPlacedPos = pos;
    }

    /**
     * Lock the current placement direction, face, and mode
     */
    public void lock(Direction offset, Direction face, PlacementMode mode) {
        this.locked = true;
        this.lockedOffset = offset;
        this.lockedFace = face;
        this.lockedMode = mode;
    }

    /**
     * Unlock the placement direction and reset last placed position
     */
    public void unlock() {
        this.locked = false;
        this.lockedOffset = null;
        this.lockedFace = null;
        this.lockedMode = null;
        this.lastPlacedPos = null;
    }
}