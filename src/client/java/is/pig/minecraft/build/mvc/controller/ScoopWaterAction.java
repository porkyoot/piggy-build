package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

public class ScoopWaterAction implements MlgAction {
    private final BlockPos targetPos;

    public ScoopWaterAction(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
