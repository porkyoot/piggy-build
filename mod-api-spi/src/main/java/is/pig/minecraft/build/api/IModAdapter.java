package is.pig.minecraft.build.api;

public interface IModAdapter {
    IInventoryManager getInventoryManager();
    IPlayerTracker getPlayerTracker();
    INetworkDispatcher getNetworkDispatcher();
}
