package is.pig.minecraft.build.api;

import java.util.UUID;

public interface IInventoryManager {
    boolean hasItem(UUID playerUuid, String itemId);
    int getItemCount(UUID playerUuid, String itemId);
    void openInventory(UUID playerUuid);
    boolean clickSlot(UUID playerUuid, int slotId, int button, int actionType);
}
