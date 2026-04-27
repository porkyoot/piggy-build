package is.pig.minecraft.build.legacy;

import is.pig.minecraft.build.api.IInventoryManager;
import java.util.UUID;

public class LegacyInventoryManager implements IInventoryManager {
    @Override
    public boolean hasItem(UUID playerUuid, String itemId) {
        // Pre-26.X: Use mapped/obfuscated methods
        return false;
    }

    @Override
    public int getItemCount(UUID playerUuid, String itemId) {
        return 0;
    }

    @Override
    public void openInventory(UUID playerUuid) {
    }

    @Override
    public boolean clickSlot(UUID playerUuid, int slotId, int button, int actionType) {
        return false;
    }
}
