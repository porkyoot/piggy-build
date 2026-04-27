package is.pig.minecraft.build.modern;

import is.pig.minecraft.build.api.IModAdapter;
import is.pig.minecraft.build.api.IInventoryManager;
import is.pig.minecraft.build.api.IPlayerTracker;
import is.pig.minecraft.build.api.INetworkDispatcher;

public class ModernModAdapter implements IModAdapter {
    private final IInventoryManager inventoryManager = new ModernInventoryManager();

    @Override
    public IInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    @Override
    public IPlayerTracker getPlayerTracker() {
        return null;
    }

    @Override
    public INetworkDispatcher getNetworkDispatcher() {
        return null;
    }
}
