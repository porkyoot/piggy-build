package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class SetRotationAction implements MlgAction {
    private final float yaw;
    private final float pitch;
    private final boolean sendPacket;

    public SetRotationAction(float yaw, float pitch, boolean sendPacket) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.sendPacket = sendPacket;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        if (this.sendPacket && client.getConnection() != null) {
            client.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        }
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
