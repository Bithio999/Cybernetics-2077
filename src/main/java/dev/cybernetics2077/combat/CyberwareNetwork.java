package dev.cybernetics2077.combat;

import dev.cybernetics2077.Cybernetics2077;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CyberwareNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};
  public static java.util.function.IntConsumer impact = i -> {};

  public record Action(int kind) implements CustomPacketPayload {
    public static final Type<Action> TYPE = new Type<>(Cybernetics2077.id("cyberware_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC =
        StreamCodec.of((b, p) -> b.writeVarInt(p.kind), b -> new Action(b.readVarInt()));

    @Override
    public Type<Action> type() {
      return TYPE;
    }
  }

  public record Hack(int target) implements CustomPacketPayload {
    public static final Type<Hack> TYPE = new Type<>(Cybernetics2077.id("quickhack"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Hack> CODEC =
        StreamCodec.of((b, p) -> b.writeVarInt(p.target), b -> new Hack(b.readVarInt()));

    @Override
    public Type<Hack> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(Cybernetics2077.id("cyberware_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }

  public record Impact(int target) implements CustomPacketPayload {
    public static final Type<Impact> TYPE = new Type<>(Cybernetics2077.id("short_circuit_impact"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Impact> CODEC =
        StreamCodec.of((b, p) -> b.writeVarInt(p.target), b -> new Impact(b.readVarInt()));

    @Override
    public Type<Impact> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("9");
    r.playToServer(
        Action.TYPE,
        Action.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer s) CyberwareRuntime.action(s, p.kind);
                }));
    r.playToServer(
        Hack.TYPE,
        Hack.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer s) CyberwareRuntime.queue(s, p.target);
                }));
    r.playToClient(
        State.TYPE,
        State.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (p.data != null) receiver.accept(p.data);
                }));
    r.playToClient(
        Impact.TYPE, Impact.CODEC, (p, c) -> c.enqueueWork(() -> impact.accept(p.target)));
  }
}
