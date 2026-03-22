package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.AttackEntityAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import is.pig.minecraft.lib.inventory.search.InventorySearcher;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class BoatMlg extends AbstractMlgMethod {

    private FallPredictionResult lastPrediction = null;

    @Override
    public boolean negatesAllDamage() {
        return true;
    }

    @Override
    public int getReliabilityScore() {
        return 80;
    }

    @Override
    public int getCleanupDifficulty() {
        return 4;
    }

    @Override
    public int getExecutionTickOffset() {
        return 3;
    }

    private int getBoatSlot(Minecraft client) {
        if (client.player == null) return -1;
        int slot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), InventorySearcher.ofClass(BoatItem.class));
        if (slot == -1) {
            slot = InventorySearcher.findSlotInMain(client.player.getInventory(), InventorySearcher.ofClass(BoatItem.class));
        }
        return slot;
    }

    @Override
    public boolean isViable(Minecraft client, FallPredictionResult prediction) {
        if (client.level == null) return false;
        
        if (getBoatSlot(client) == -1) return false;

        BlockPos landingPos = prediction.landingPos();
        net.minecraft.world.level.block.state.BlockState landingState = client.level.getBlockState(landingPos);
        
        if (landingState.isAir() || landingState.canBeReplaced()) {
            return false;
        }

        // 3x3 clear space verification to ensure boat doesn't suffocate/fail
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = landingPos.offset(x, 1, z);
                if (!client.level.getBlockState(checkPos).getCollisionShape(client.level, checkPos).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void queuePreparationActions(PiggyActionQueue queue) {
        Minecraft client = Minecraft.getInstance();
        int slot = getBoatSlot(client);
        
        if (slot != -1) {
            queue.enqueue(new SelectHotbarSlotAction(slot, "piggy-build") {
                @Override
                public ActionPriority getPriority() {
                    return ActionPriority.HIGHEST;
                }
            });
        }

        float yaw = client.player != null ? client.player.getYRot() : 0.0f;
        queue.enqueue(new SetRotationAction(90.0f, yaw, 2, "piggy-build") {
            @Override
            public ActionPriority getPriority() {
                return ActionPriority.HIGHEST;
            }
        });
    }

    @Override
    public void queueExecutionActions(PiggyActionQueue queue, FallPredictionResult prediction) {
        this.lastPrediction = prediction;
        Minecraft client = Minecraft.getInstance();
        BlockPos pos = prediction.landingPos();
        BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        BooleanSupplier verifySuccess = () -> client.level != null && !client.level.getEntitiesOfClass(
                Boat.class,
                new AABB(pos).inflate(2),
                e -> true).isEmpty();

        queue.enqueue(new InteractBlockAction(hitResult, InteractionHand.MAIN_HAND, "piggy-build", verifySuccess) {
            @Override
            public ActionPriority getPriority() {
                return ActionPriority.HIGHEST;
            }
        });
    }

    @Override
    public void queueCleanupActions(PiggyActionQueue queue) {
        Minecraft client = Minecraft.getInstance();
        
        Supplier<Entity> entityLocator = () -> {
            if (client.level == null || client.player == null) return null;
            
            List<Boat> boats = client.level.getEntitiesOfClass(
                    Boat.class,
                    client.player.getBoundingBox().inflate(4),
                    e -> true);
            
            if (boats.isEmpty()) return null;
            
            Boat closest = null;
            double minDistance = Double.MAX_VALUE;
            for (Boat boat : boats) {
                double dist = boat.distanceToSqr(client.player);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = boat;
                }
            }
            return closest;
        };

        queue.enqueue(new AttackEntityAction(entityLocator, "piggy-build", ActionPriority.HIGHEST));
    }
}
