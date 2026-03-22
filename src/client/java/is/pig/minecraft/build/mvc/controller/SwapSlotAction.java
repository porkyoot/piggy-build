package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public class SwapSlotAction implements MlgAction {
    private final int slot;

    public SwapSlotAction(int slot) {
        this.slot = slot;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        if (this.slot >= 0 && this.slot < 9 && player.getInventory().selected != this.slot) {
            player.getInventory().selected = this.slot;
            if (client.getConnection() != null) {
                client.getConnection().send(new ServerboundSetCarriedItemPacket(this.slot));
            }
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
