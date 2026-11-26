package dev.cybernetics2077.city;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Dossiers are sent only to the player who has that fixer's menu open. */
public final class BountyNetwork {
  public record State(int menu, CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(Cybernetics2077.id("bounty_board"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeVarInt(p.menu);
              b.writeNbt(p.data);
            },
            b -> new State(b.readVarInt(), b.readNbt()));

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    e.registrar("1")
        .playToClient(
            State.TYPE,
            State.CODEC,
            (packet, context) ->
                context.enqueueWork(
                    () -> {
                      if (packet.data != null
                          && context.player().containerMenu instanceof FixerMenu menu
                          && menu.containerId == packet.menu) menu.board(packet.data);
                    }));
  }

  private BountyNetwork() {}
}
