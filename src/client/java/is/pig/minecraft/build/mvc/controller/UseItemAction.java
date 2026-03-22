package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;

public class UseItemAction implements MlgAction {
    private final InteractionHand hand;

    public UseItemAction(InteractionHand hand) {
        this.hand = hand;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        client.gameMode.useItem(player, hand);
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
