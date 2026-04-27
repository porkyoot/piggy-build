package is.pig.minecraft.build.api;

import java.util.UUID;

public interface INetworkDispatcher {
    void sendPayload(UUID playerUuid, String channel, byte[] data);
    void broadcastPayload(String channel, byte[] data);
}
