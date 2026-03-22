package is.pig.minecraft.build.mvc.controller;

import is.pig.minecraft.build.PiggyBuildClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

public class MlgItemEvaluator {

    public static boolean isSafePearlLanding(BlockState state) {
        if (state.getFluidState().is(net.minecraft.world.level.material.Fluids.LAVA) || state.getFluidState().is(net.minecraft.world.level.material.Fluids.FLOWING_LAVA)) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.CACTUS)) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.CAMPFIRE) || state.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)) return false;
        if (state.is(net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE)) return false;
        return true;
    }

    public static boolean isValidMlg(Item item, LocalPlayer player, float fallDist, BlockState impactState, BlockPos placePos) {
        if (item == net.minecraft.world.item.Items.CHORUS_FRUIT) return true;
        if (item == net.minecraft.world.item.Items.WATER_BUCKET) {
            if (player.level().dimension() == net.minecraft.world.level.Level.NETHER) return false;
            return true;
        }
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) {
            return player.getHealth() > 6.0f && fallDist <= 25.0f;
        }
        if (item == net.minecraft.world.item.Items.ENDER_PEARL) {
            if (impactState != null && !isSafePearlLanding(impactState)) return false;
            return player.getHealth() > 6.0f;
        }
        if (item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET) return true;
        if (item instanceof net.minecraft.world.item.BoatItem) return true;
        if (item == net.minecraft.world.item.Items.SLIME_BLOCK) return true;
        if (item == net.minecraft.world.item.Items.COBWEB) return true;
        if (item == net.minecraft.world.item.Items.TWISTING_VINES) return true;

        if (item == net.minecraft.world.item.Items.LADDER || item == net.minecraft.world.item.Items.VINE) {
            if (placePos == null || player.level() == null) return false;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = placePos.relative(dir);
                if (player.level().getBlockState(neighbor).isFaceSturdy(player.level(), neighbor, dir.getOpposite()) || player.level().getBlockState(neighbor).isCollisionShapeFullBlock(player.level(), neighbor)) {
                    return true;
                }
            }
            return false;
        }

        if (item == net.minecraft.world.item.Items.HAY_BLOCK) {
            return true;
        }

        if (item instanceof net.minecraft.world.item.BedItem) {
            return true;
        }

        return false;
    }

    public static int getMlgPriorityRank(Item item, BlockState impactState) {
        if (item == net.minecraft.world.item.Items.WATER_BUCKET) {
            if (impactState != null && impactState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                return 8; // Worst priority!
            }
            return 1;
        }
        if (item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET) return 2;
        if (item instanceof net.minecraft.world.item.BoatItem) return 3;
        if (item == net.minecraft.world.item.Items.SLIME_BLOCK) return 4;
        if (item == net.minecraft.world.item.Items.COBWEB) return 5;
        if (item == net.minecraft.world.item.Items.TWISTING_VINES) return 6;
        if (item == net.minecraft.world.item.Items.HAY_BLOCK) return 7;
        if (item instanceof net.minecraft.world.item.BedItem) return 8;
        if (item == net.minecraft.world.item.Items.LADDER || item == net.minecraft.world.item.Items.VINE) return 10;
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) return 15;
        if (item == net.minecraft.world.item.Items.ENDER_PEARL) return 20;
        if (item == net.minecraft.world.item.Items.CHORUS_FRUIT) return 25;
        return 99;
    }

    public static int findMlgItem(LocalPlayer player, Minecraft client, float predictedFallDist, BlockState impactState, BlockPos placePos, java.util.List<Integer> failedSlots) {
        java.util.List<Integer> validSlots = new java.util.ArrayList<>();

        for (int i = 0; i < 36; i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (failedSlots != null && failedSlots.contains(i)) continue;
            
            Item item = stack.getItem();

            if (item == net.minecraft.world.item.Items.WATER_BUCKET) {
                if (player.level().dimension() == net.minecraft.world.level.Level.NETHER) continue;
                if (impactState != null && impactState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) continue;
                validSlots.add(i);
                continue;
            }

            if (item == net.minecraft.world.item.Items.LAVA_BUCKET) {
                if (player.getHealth() <= 6.0f || predictedFallDist > 25.0f) continue;
                validSlots.add(i);
                continue;
            }

            if (item == net.minecraft.world.item.Items.ENDER_PEARL) {
                if (player.getHealth() <= 6.0f) continue;
                if (impactState != null && !isSafePearlLanding(impactState)) continue;
                validSlots.add(i);
                continue;
            }

            if (item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET ||
                item instanceof net.minecraft.world.item.BoatItem ||
                item == net.minecraft.world.item.Items.SLIME_BLOCK ||
                item == net.minecraft.world.item.Items.COBWEB ||
                item == net.minecraft.world.item.Items.TWISTING_VINES ||
                item == net.minecraft.world.item.Items.HAY_BLOCK ||
                item instanceof net.minecraft.world.item.BedItem ||
                item == net.minecraft.world.item.Items.CHORUS_FRUIT) {
                validSlots.add(i);
                continue;
            }

            if (item == net.minecraft.world.item.Items.LADDER || item == net.minecraft.world.item.Items.VINE) {
                if (placePos == null || player.level() == null) continue;
                boolean validPlacement = false;
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = placePos.relative(dir);
                    if (player.level().getBlockState(neighbor).isFaceSturdy(player.level(), neighbor, dir.getOpposite()) || player.level().getBlockState(neighbor).isCollisionShapeFullBlock(player.level(), neighbor)) {
                        validPlacement = true;
                        break;
                    }
                }
                if (!validPlacement) continue;
                validSlots.add(i);
                continue;
            }
        }

        if (validSlots.isEmpty()) {
            return -1;
        }

        java.util.Optional<Integer> bestSlotOpt = validSlots.stream()
            .max(java.util.Comparator.comparing((Integer slot) -> isPerfectCounter(client, player, slot, impactState))
                .thenComparing(slot -> isPrimaryLiquid(player, slot))
                .thenComparing(slot -> isSolidBounce(player, slot))
                .thenComparing(slot -> isVehicle(player, slot))
                .thenComparing(slot -> isEmergencyProjectile(player, slot))
                .thenComparing(slot -> slot == player.getInventory().selected)
                .thenComparing(slot -> slot < 9));

        if (!bestSlotOpt.isPresent()) {
            return -1;
        }

        return bestSlotOpt.get();
    }

    private static boolean isPerfectCounter(Minecraft client, LocalPlayer player, int slot, BlockState impactState) {
        Item item = player.getInventory().getItem(slot).getItem();
        if (impactState != null) {
            if (impactState.is(net.minecraft.world.level.block.Blocks.LAVA)) {
                return isSolidBounce(player, slot) || isVehicle(player, slot) || item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET;
            }
            if (impactState.is(net.minecraft.world.level.block.Blocks.FIRE) || impactState.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                return item == net.minecraft.world.item.Items.WATER_BUCKET || item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET;
            }
        }
        return false;
    }

    private static boolean isPrimaryLiquid(LocalPlayer player, int slot) {
        Item item = player.getInventory().getItem(slot).getItem();
        return item == net.minecraft.world.item.Items.WATER_BUCKET || item == net.minecraft.world.item.Items.POWDER_SNOW_BUCKET;
    }

    private static boolean isSolidBounce(LocalPlayer player, int slot) {
        Item item = player.getInventory().getItem(slot).getItem();
        return item == net.minecraft.world.item.Items.SLIME_BLOCK || item == net.minecraft.world.item.Items.HAY_BLOCK || item instanceof net.minecraft.world.item.BedItem;
    }

    private static boolean isVehicle(LocalPlayer player, int slot) {
        Item item = player.getInventory().getItem(slot).getItem();
        return item instanceof net.minecraft.world.item.BoatItem;
    }

    private static boolean isEmergencyProjectile(LocalPlayer player, int slot) {
        Item item = player.getInventory().getItem(slot).getItem();
        return item == net.minecraft.world.item.Items.ENDER_PEARL || item == net.minecraft.world.item.Items.CHORUS_FRUIT;
    }

    public static int findEmptyBucket(LocalPlayer player, int mlgSlot) {
        for (int i = 0; i < 9; i++) {
           if (player.getInventory().getItem(i).getItem() == net.minecraft.world.item.Items.BUCKET) return i;
        }
        if (mlgSlot != -1 && player.getInventory().getItem(mlgSlot).getItem() == net.minecraft.world.item.Items.BUCKET) {
            return mlgSlot;
        }
        return -1;
    }
}
