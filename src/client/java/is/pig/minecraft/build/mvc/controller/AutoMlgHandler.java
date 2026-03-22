package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.mlg.statemachine.MlgStateMachine;
import net.minecraft.client.Minecraft;

public class AutoMlgHandler {
    
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        
        if (client.player.onGround() || client.player.getDeltaMovement().y >= 0 || client.player.isFallFlying()) {
            return;
        }

        MlgStateMachine.getInstance().tick(client);
    }
}
