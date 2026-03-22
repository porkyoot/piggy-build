package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.build.mlg.statemachine.MlgStateMachine;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import is.pig.minecraft.lib.action.world.UseItemAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BooleanSupplier;

public class WaterBucketMlg extends AbstractMlgMethod {

    private FallPredictionResult lastPrediction = null;

    @Override
    public boolean negatesAllDamage() {
        return true;
    }

    @Override
    public int getReliabilityScore() {
        return 100;
    }

    @Override
    public int getCleanupDifficulty() {
        return 1;
    }

    @Override
    public int getExecutionTickOffset() {
        return 2;
    }

    @Override
    public boolean isViable(Minecraft client, FallPredictionResult prediction) {
        if (client.level == null) return false;
        
        int slot = findItemSlot(client, Items.WATER_BUCKET);
        if (slot == -1) return false;

        if (client.level.dimensionType().ultraWarm()) {
            return false;
        }

        BlockPos landingPos = prediction.landingPos();
        net.minecraft.world.level.block.state.BlockState landingState = client.level.getBlockState(landingPos);
        
        if (landingState.isAir() || landingState.canBeReplaced()) {
            return false;
        }

        return true;
    }

    @Override
    public void queuePreparationActions(PiggyActionQueue queue) {
        Minecraft client = Minecraft.getInstance();
        int slot = findItemSlot(client, Items.WATER_BUCKET);
        
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

        BooleanSupplier verifySuccess = () -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.WATER);

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
        
        BooleanSupplier verifyScooped = () -> {
            if (this.lastPrediction == null) return false;
            return client.level != null && client.level.getBlockState(this.lastPrediction.landingPos().above()).isAir();
        };

        queue.enqueue(new UseItemAction(InteractionHand.MAIN_HAND, "piggy-build", verifyScooped) {
            @Override
            public ActionPriority getPriority() {
                return ActionPriority.NORMAL;
            }
        });
    }
}
