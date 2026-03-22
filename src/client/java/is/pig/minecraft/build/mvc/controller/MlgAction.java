package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public interface MlgAction extends Comparable<MlgAction> {
    void execute(Minecraft client, LocalPlayer player);
    int getPriority();
    default int getDelay() { return 1; }

    @Override
    default int compareTo(MlgAction other) {
        return Integer.compare(this.getPriority(), other.getPriority());
    }
}
