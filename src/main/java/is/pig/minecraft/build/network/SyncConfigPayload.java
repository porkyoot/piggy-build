package is.pig.minecraft.build.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncConfigPayload(boolean allowCheats) implements CustomPacketPayload {
    public static final Type<SyncConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("piggy-build", "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncConfigPayload> CODEC = CustomPacketPayload.codec(
            SyncConfigPayload::write,
            SyncConfigPayload::new);

    public SyncConfigPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(allowCheats);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
