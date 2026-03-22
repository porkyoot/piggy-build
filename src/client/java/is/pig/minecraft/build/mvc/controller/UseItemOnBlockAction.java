package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class UseItemOnBlockAction implements MlgAction {
    private final InteractionHand hand;
    private final BlockHitResult hitResult;

    public UseItemOnBlockAction(InteractionHand hand, BlockHitResult hitResult) {
        this.hand = hand;
        this.hitResult = hitResult;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        if (!client.gameMode.useItemOn(player, hand, hitResult).consumesAction()) {
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundUseItemOnPacket(hand, hitResult, 0));
            }
        }
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
