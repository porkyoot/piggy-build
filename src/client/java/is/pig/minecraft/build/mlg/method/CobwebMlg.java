package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.build.mlg.prediction.FallPredictionResult;
import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.inventory.SelectHotbarSlotAction;
import is.pig.minecraft.lib.action.player.SetRotationAction;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
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

public class CobwebMlg extends AbstractMlgMethod {

    private FallPredictionResult lastPrediction = null;

    @Override
    public boolean negatesAllDamage() {
        return true;
    }

    @Override
    public int getReliabilityScore() {
        return 90;
    }

    @Override
    public int getCleanupDifficulty() {
        return 5;
    }

    @Override
    public int getExecutionTickOffset() {
        return 2;
    }

    @Override
    public boolean isViable(Minecraft client, FallPredictionResult prediction) {
        if (client.level == null) return false;
        
        int slot = findItemSlot(client, Items.COBWEB);
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
        int slot = findItemSlot(client, Items.COBWEB);
        
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

        BooleanSupplier verifySuccess = () -> client.level != null && client.level.getBlockState(pos.above()).is(Blocks.COBWEB);

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

        try {
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("piggy-inventory")) {
                Class<?> handlerClass = Class.forName("is.pig.minecraft.inventory.mvc.controller.ToolSwapHandler");
                Object handlerInstance = handlerClass.getDeclaredConstructor().newInstance();
                java.lang.reflect.Method getBestToolSlotMethod = handlerClass.getMethod("getBestToolSlot", Minecraft.class, net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class);
                int bestSlot = (Integer) getBestToolSlotMethod.invoke(handlerInstance, client, targetPos, Blocks.COBWEB.defaultBlockState());

                if (bestSlot != -1 && client.player != null && bestSlot != client.player.getInventory().selected) {
                    queue.enqueue(new SelectHotbarSlotAction(bestSlot, "piggy-build") {
                        @Override
                        public ActionPriority getPriority() {
                            return ActionPriority.NORMAL;
                        }
                    });
                }
            }
        } catch (Exception t) {
            // Silently fallback if piggy-inventory isn't linked/available successfully
        }

        queue.enqueue(new BreakBlockAction(targetPos, "piggy-build", ActionPriority.NORMAL));
    }
}
