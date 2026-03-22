package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import java.util.PriorityQueue;

public class MlgActionQueue {
    private final PriorityQueue<MlgAction> queue = new PriorityQueue<>();
    private int delayTicks = 0;

    public void enqueue(MlgAction action) {
        queue.add(action);
    }

    public boolean processNext(Minecraft client, LocalPlayer player) {
        if (delayTicks > 0) {
            delayTicks--;
            return true;
        }

        MlgAction action = queue.poll();
        if (action != null) {
            action.execute(client, player);
            delayTicks = action.getDelay();
            return true;
        }
        
        return false;
    }

    public void clearQueue() {
        queue.clear();
        delayTicks = 0;
    }

    public boolean isEmpty() {
        return queue.isEmpty() && delayTicks == 0;
    }
}
