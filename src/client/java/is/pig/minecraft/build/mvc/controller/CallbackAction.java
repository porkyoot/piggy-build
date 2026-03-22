package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class CallbackAction implements MlgAction {
    private final Runnable callback;
    private final int priority;

    public CallbackAction(Runnable callback, int priority) {
        this.callback = callback;
        this.priority = priority;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        callback.run();
    }

    @Override
    public int getPriority() {
        return priority;
    }
}
