package is.pig.minecraft.build.mlg.method;

import is.pig.minecraft.lib.inventory.search.InventorySearcher;
import is.pig.minecraft.lib.inventory.search.ItemCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;

public abstract class AbstractMlgMethod implements MlgMethod {

    public abstract boolean negatesAllDamage();

    public abstract int getReliabilityScore();

    public abstract int getCleanupDifficulty();

    protected int findItemSlot(Minecraft client, Item targetItem) {
        if (client.player == null) return -1;
        
        ItemCondition condition = stack -> stack.getItem() == targetItem;
        
        int hotbarSlot = InventorySearcher.findSlotInHotbar(client.player.getInventory(), condition);
        if (hotbarSlot != -1) {
            return hotbarSlot;
        }
        
        return InventorySearcher.findSlotInMain(client.player.getInventory(), condition);
    }
}
