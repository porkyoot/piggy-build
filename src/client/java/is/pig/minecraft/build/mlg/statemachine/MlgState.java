package is.pig.minecraft.build.mlg.statemachine;
import is.pig.minecraft.api.*;

/**
 * Represents the current phase of the MLG process.
 */
public enum MlgState {
    /**
     * Player is safe, not falling fatally.
     */
    IDLE,

    /**
     * Fatal fall detected, system is evaluating options.
     */
    FALLING,

    /**
     * Method selected, swapping tools and rotating camera.
     */
    PREPARATION,

    /**
     * Waiting for the exact tick to perform the placement.
     */
    EXECUTION,

    /**
     * Placement done, waiting to land safely or bounce.
     */
    RECOVERY
}
