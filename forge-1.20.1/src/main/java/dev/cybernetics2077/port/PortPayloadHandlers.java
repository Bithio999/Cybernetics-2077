package dev.cybernetics2077.port;

import java.util.*;
import java.util.function.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.*;

public final class PortPayloadHandlers {
  static final net.minecraftforge.network.simple.SimpleChannel CHANNEL =
      NetworkRegistry.newSimpleChannel(
          new net.minecraft.resources.ResourceLocation("cybernetics2077", "main"),
          () -> "20",
          "20"::equals,
          "20"::equals);
  private static int next;
  private static final Map<PortPayload.Type<?>, Integer> IDS = new HashMap<>();

  public PortPayloadHandlers registrar(String version) {
    return this;
  }

  public <T extends PortPayload> void playToServer(
      PortPayload.Type<T> type,
      PortStreamCodec<FriendlyByteBuf, T> codec,
      BiConsumer<T, Context> handler) {
    add(type, codec, handler, NetworkDirection.PLAY_TO_SERVER);
  }

  public <T extends PortPayload> void playToClient(
      PortPayload.Type<T> type,
      PortStreamCodec<FriendlyByteBuf, T> codec,
      BiConsumer<T, Context> handler) {
    add(type, codec, handler, NetworkDirection.PLAY_TO_CLIENT);
  }

  @SuppressWarnings("unchecked")
  private <T extends PortPayload> void add(
      PortPayload.Type<T> type,
      PortStreamCodec<FriendlyByteBuf, T> codec,
      BiConsumer<T, Context> handler,
      NetworkDirection direction) {
    int id = next++;
    IDS.put(type, id);
    CHANNEL.registerMessage(
        id,
        type.javaType(),
        (p, b) -> codec.encode().accept(b, p),
        codec.decode(),
        (p, s) -> {
          var c = s.get();
          handler.accept(p, new Context(c));
          c.setPacketHandled(true);
        },
        Optional.of(direction));
  }

  public record Context(NetworkEvent.Context context) {
    public net.minecraft.world.entity.player.Player player() {
      return context.getSender();
    }

    public void enqueueWork(Runnable work) {
      context.enqueueWork(work);
    }
  }

  public static void sendToServer(PortPayload p) {
    send(p, null, NetworkDirection.PLAY_TO_SERVER);
  }

  public static void sendToPlayer(ServerPlayer player, PortPayload p) {
    send(p, player, NetworkDirection.PLAY_TO_CLIENT);
  }

  public static void sendToPlayersTrackingEntityAndSelf(Entity entity, PortPayload p) {
    if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
      level
          .getChunkSource()
          .broadcastAndSend(entity, CHANNEL.toVanillaPacket(p, NetworkDirection.PLAY_TO_CLIENT));
    }
  }

  private static void send(PortPayload p, ServerPlayer player, NetworkDirection direction) {
    // Reject accidental sends before the fixed packet registration pass.
    Integer id = IDS.get(p.type());
    if (id == null) throw new IllegalArgumentException("Unregistered payload");
    var packet = CHANNEL.toVanillaPacket(p, direction);
    if (player != null) player.connection.send(packet);
    else net.minecraft.client.Minecraft.getInstance().getConnection().send(packet);
  }
}
