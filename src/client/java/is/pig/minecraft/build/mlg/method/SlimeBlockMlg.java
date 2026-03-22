package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BooleanSupplier;

public class SlimeBlockMlg extends AbstractMlgMethod {

    private FallPredictionResult lastPrediction = null;

    @Override
    public boolean negatesAllDamage() {
        return true;
    }

    @Override
    public int getReliabilityScore() {
        return 95;
    }

    @Override
    public int getCleanupDifficulty() {
        return 3;
    }

    @Override
    public int getExecutionTickOffset() {
        return 2;
    }

    @Override
    public boolean isViable(Minecraft client, FallPredictionResult prediction) {
        if (client.level == null) return false;
        
        int slot = findItemSlot(client, Items.SLIME_BLOCK);
        if (slot == -1) return false;

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
        int slot = findItemSlot(client, Items.SLIME_BLOCK);
        
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

        BooleanSupplier verifySuccess = () -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.SLIME_BLOCK);

        queue.enqueue(new InteractBlockAction(hitResult, InteractionHand.MAIN_HAND, "piggy-build", verifySuccess) {
            @Override
            public ActionPriority getPriority() {
                return ActionPriority.HIGHEST;
            }
        });
    }

    @Override
    public void queueCleanupActions(PiggyActionQueue queue) {
        if (this.lastPrediction == null) return;
        Minecraft client = Minecraft.getInstance();
        BlockPos targetPos = this.lastPrediction.landingPos().above();

        queue.enqueue(new AbstractAction("piggy-build", ActionPriority.NORMAL) {
            private int ticksMining = 0;

            @Override
            public boolean execute(Minecraft client) {
                if (client.gameMode != null && client.player != null) {
                    if (ticksMining == 0) {
                        client.gameMode.startDestroyBlock(targetPos, Direction.UP);
                    } else {
                        client.gameMode.continueDestroyBlock(targetPos, Direction.UP);
                    }
                    client.player.swing(InteractionHand.MAIN_HAND);
                }
                ticksMining++;
                
                if (verify(client)) return true;
                if (ticksMining > 200) return true; // Timeout max 10 seconds
                return false;
            }

            @Override
            protected void onExecute(Minecraft client) {}

            @Override
            protected boolean verify(Minecraft client) {
                return client.level != null && client.level.getBlockState(targetPos).isAir();
            }

            @Override
            public String getName() {
                return "Break Slime Block";
            }
            
            @Override
            public ActionPriority getPriority() {
                return ActionPriority.NORMAL;
            }
        });
    }
}
