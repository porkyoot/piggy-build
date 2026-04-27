package is.pig.minecraft.build.api;

/**
 * Pure Java interface for world queries and actions.
 */
public interface IWorldInteraction {
    IVector3 raycast(double distance);
    String getFluidAt(int x, int y, int z);
    void placeBlock(int x, int y, int z, String itemId);
}
