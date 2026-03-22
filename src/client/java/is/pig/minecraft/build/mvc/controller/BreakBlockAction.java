package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BreakBlockAction implements MlgAction {
    private final BlockPos targetPos;

    public BreakBlockAction(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        client.gameMode.startDestroyBlock(targetPos, Direction.UP);
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
