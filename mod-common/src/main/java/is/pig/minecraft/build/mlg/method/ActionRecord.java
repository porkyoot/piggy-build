package is.pig.minecraft.build.mlg.method;

/**
 * Generic payload for MLG actions.
 */
public record ActionRecord(String actionType, int x, int y, int z, String itemId) {}
