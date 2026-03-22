package is.pig.minecraft.build.mvc.controller;

import net.minecraft.core.BlockPos;

public class MlgContext {
    public enum MlgType {
        SOLID,
        LIQUID,
        BOAT,
        PEARL
    }

    public BlockPos targetPos;
    public BlockPos impactPos;
    public int mlgItemSlot = -1;
    public int originalSlot = -1;
    public MlgType mlgType;
    public boolean isBouncing;
    public java.util.List<Integer> failedSlots = new java.util.ArrayList<>();
}
