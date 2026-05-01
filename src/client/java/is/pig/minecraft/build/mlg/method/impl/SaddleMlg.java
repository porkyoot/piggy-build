package is.pig.minecraft.build.mlg.method.impl;
import is.pig.minecraft.api.*;

import is.pig.minecraft.build.mlg.method.ComposedMlgMethod;
import is.pig.minecraft.build.mlg.method.MlgMethod;
import is.pig.minecraft.build.mlg.method.strategy.CommonMlgStrategies;
import is.pig.minecraft.build.mlg.method.strategy.MlgViabilityStrategy;
import is.pig.minecraft.build.mlg.method.strategy.MlgExecutionStrategy;
import is.pig.minecraft.api.AbstractAction;
import is.pig.minecraft.api.ActionPriority;
import is.pig.minecraft.inventory.util.InventorySearcher;
import is.pig.minecraft.inventory.util.ItemCondition;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import is.pig.minecraft.lib.util.TypeConverter;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SaddleMlg {
    public static MlgMethod create() {
        return ComposedMlgMethod.builder()
            .negatesAllDamage(true)
            .reliabilityScore(90)
            .cleanupDifficulty(0)
            .preparationTickOffset(CommonMlgStrategies.dynamicPreparation())
            .executionCondition(CommonMlgStrategies.dynamicReach())
            .viability(requireSaddleAndUnsaddledAnimal())
            .preparation(CommonMlgStrategies.swapToItemAndLookDown(Items.SADDLE))
            .execution(saddleAndMountEntity())
            .cleanup(CommonMlgStrategies.noCleanup())
            .build();
    }

    private static MlgViabilityStrategy requireSaddleAndUnsaddledAnimal() {
        return (client, prediction) -> {
            if (client.level == null || client.player == null) return false;
            ItemCondition saddleCondition = stack -> PiggyServiceRegistry.getItemDataAdapter().isItem(stack, Items.SADDLE.toString());
            boolean hasSaddle = saddleCondition.matches(client.player.getOffhandItem()) || 
                                InventorySearcher.findSlotInHotbar(client.player.getInventory(), saddleCondition) != -1 ||
                                InventorySearcher.findSlotInMain(client.player.getInventory(), saddleCondition) != -1;
            if (!hasSaddle) return false;

            List<? extends Entity> entities = client.level.getEntitiesOfClass(
                    Entity.class, 
                    new AABB(TypeConverter.toMinecraft(prediction.landingPos())).inflate(3), 
                    e -> {
                        if (e.hasPassenger(client.player)) return false;
                        if (!e.getPassengers().isEmpty()) return false;
                        if (e instanceof Saddleable s && !s.isSaddled()) return true;
                        if (e instanceof AbstractHorse h && h.isTamed() && !h.isSaddled()) return true;
                        return false;
                    }
            );
            return !entities.isEmpty();
        };
    }

    private static MlgExecutionStrategy saddleAndMountEntity() {
        return (queue, client, prediction) -> {
            Predicate<ItemStack> itemCondition = stack -> stack.is(Items.SADDLE);
            queue.enqueue(new AbstractAction("piggy-build", ActionPriority.HIGHEST) {
                private long start = System.currentTimeMillis();
                @Override
                protected void onExecute(Object clientObj) {
        Minecraft c = (Minecraft) clientObj;
                    if (c.level == null || c.player == null) return;
                    var entities = c.level.getEntitiesOfClass(Entity.class, new AABB(TypeConverter.toMinecraft(prediction.landingPos())).inflate(3), e -> {
                        if (e instanceof Saddleable s && !s.isSaddled()) return true;
                        if (e instanceof AbstractHorse h && h.isTamed() && !h.isSaddled()) return true;
                        return false;
                    });
                    if (!entities.isEmpty()) {
                        Entity target = entities.get(0);
                        double dist = c.player.getEyePosition().distanceTo(target.position());
                        if (dist > 3.0 && c.getConnection() != null) {
                            double targetEyeY = target.getY() + 3.0;
                            double targetFeetY = targetEyeY - c.player.getEyeHeight();
                            if (targetFeetY <= c.player.getY() && targetFeetY > target.getY()) {
                                c.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                                        c.player.getX(), targetFeetY, c.player.getZ(), c.player.onGround()));
                            }
                        }
                        InteractionHand hand = InteractionHand.MAIN_HAND;
                        if (c.player != null && itemCondition.test(c.player.getOffhandItem())) {
                            hand = InteractionHand.OFF_HAND;
                        }
                        c.gameMode.interactAt(c.player, target, new EntityHitResult(target), hand);
                        c.gameMode.interact(c.player, target, hand);
                        c.player.swing(hand);
                    }
                }
                @Override
                protected Optional<Boolean> verify(Object clientObj) {
        Minecraft c = (Minecraft) clientObj;
                    if (c.level == null || c.player == null) return Optional.of(false);
                    var entities = c.level.getEntitiesOfClass(Entity.class, new AABB(TypeConverter.toMinecraft(prediction.landingPos())).inflate(3), e -> {
                        if (e instanceof Saddleable s && s.isSaddled()) return true;
                        if (e instanceof AbstractHorse h && h.isSaddled()) return true;
                        return false;
                    });
                    if (!entities.isEmpty()) return Optional.of(true);
                    if (System.currentTimeMillis() - start > 1000) return Optional.of(false);
                    onExecute(c);
                    return Optional.empty();
                }
                @Override
                public String getName() { return "Saddle Entity"; }
                @Override
                public boolean isClick() { return true; }
            });

            queue.enqueue(CommonMlgStrategies.createSpamMountAction(prediction, null, e -> {
                if (e instanceof Saddleable s && s.isSaddled()) return true;
                if (e instanceof AbstractHorse h && h.isSaddled()) return true;
                return false;
            }));
        };
    }
}
