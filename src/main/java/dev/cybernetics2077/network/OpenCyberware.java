package dev.cybernetics2077.network;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenCyberware() implements CustomPacketPayload {
  public static final OpenCyberware INSTANCE = new OpenCyberware();
  public static final Type<OpenCyberware> TYPE = new Type<>(Cybernetics2077.id("open_cyberware"));
  public static final StreamCodec<RegistryFriendlyByteBuf, OpenCyberware> CODEC =
      StreamCodec.unit(INSTANCE);

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }
}
