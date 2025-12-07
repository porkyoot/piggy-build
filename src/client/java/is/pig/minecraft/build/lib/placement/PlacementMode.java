package is.pig.minecraft.build.lib.placement;

/**
 * Different modes for flexible block placement
 */
public enum PlacementMode {
    /**
     * Normal vanilla placement - no modification
     */
    VANILLA,

    /**
     * Directional placement - place on edge faces or behind center
     * - Edge click: place on that edge's adjacent face
     * - Center click: place behind the block (opposite face)
     */
    DIRECTIONAL,

    /**
     * Diagonal placement - always place next to the current block
     * Creates diagonal/stair patterns based on where you click
     * - Edge click: place diagonally adjacent (two faces)
     * - Center click: place directly adjacent on same face
     */
    DIAGONAL
}