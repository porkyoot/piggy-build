package is.pig.minecraft.build.mvc.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.vehicle.Boat;

public class CleanBoatAction implements MlgAction {
    private final BlockPos searchPos;

    public CleanBoatAction(BlockPos searchPos) {
        this.searchPos = searchPos;
    }

    @Override
    public void execute(Minecraft client, LocalPlayer player) {
        AABB box = new AABB(searchPos).inflate(3.0);
        for (Boat boat : client.level.getEntitiesOfClass(Boat.class, box)) {
            if (!boat.hasPassenger(player)) {
                if (client.getConnection() != null) {
                    client.getConnection().send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createAttackPacket(boat, player.isShiftKeyDown()));
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
