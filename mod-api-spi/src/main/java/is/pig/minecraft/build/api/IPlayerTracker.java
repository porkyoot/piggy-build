package is.pig.minecraft.build.api;

import java.util.UUID;
import java.util.List;

public interface IPlayerTracker {
    boolean isPlayerOnline(UUID playerUuid);
    String getPlayerName(UUID playerUuid);
    List<UUID> getOnlinePlayers();
}
