package dev.cybernetics2077.scanner;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.CyberwareCategory;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class ScannerNetwork {
  public static Consumer<CompoundTag> receiver = t -> {};
  private static final Map<ServerPlayer, Integer> SENT = new WeakHashMap<>();
  private static final Map<ServerPlayer, Long> LAST = new WeakHashMap<>();

  public record Request(int target) implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(Cybernetics2077.id("scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
        StreamCodec.of((b, p) -> b.writeVarInt(p.target), b -> new Request(b.readVarInt()));

    @Override
    public Type<Request> type() {
      return TYPE;
    }
  }

  public record State(CompoundTag data) implements CustomPacketPayload {
    public static final Type<State> TYPE = new Type<>(Cybernetics2077.id("scanner_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC =
        StreamCodec.of((b, p) -> b.writeNbt(p.data), b -> new State(b.readNbt()));

    @Override
    public Type<State> type() {
      return TYPE;
    }
  }

  public static boolean installed(net.minecraft.world.entity.player.Player player) {
    return tier(player) != null;
  }

  public static dev.cybernetics2077.cyberware.CyberwareTier tier(
      net.minecraft.world.entity.player.Player player) {
    var stack =
        player
            .getData(Cybernetics2077.CYBERWARE)
            .getStackInSlot(CyberwareCategory.FACE.firstSlot());
    return stack.getItem() instanceof dev.cybernetics2077.cyberware.OpticsItem item
        ? item.tier()
        : stack.getItem() instanceof dev.cybernetics2077.cyberware.AugmentItem item
                && item.augment() == dev.cybernetics2077.cyberware.Augment.CLAIRVOYANT
            ? item.tier()
            : null;
  }

  public static int range(net.minecraft.world.entity.player.Player player) {
    var tier = tier(player);
    return tier == null ? 0 : tier.scanRange();
  }

  public static void register(RegisterPayloadHandlersEvent e) {
    var r = e.registrar("3");
    r.playToServer(
        Request.TYPE,
        Request.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (c.player() instanceof ServerPlayer s) request(s, p.target);
                }));
    r.playToClient(
        State.TYPE,
        State.CODEC,
        (p, c) ->
            c.enqueueWork(
                () -> {
                  if (p.data != null) receiver.accept(p.data);
                }));
  }

  public static void syncIfChanged(ServerPlayer p) {
    if (!Objects.equals(SENT.get(p), range(p))) sync(p);
  }

  public static void sync(ServerPlayer p) {
    boolean value = installed(p);
    SENT.put(p, range(p));
    var t = new CompoundTag();
    t.putBoolean("installed", value);
    t.putInt("range", range(p));
    t.put("contract", dev.cybernetics2077.city.DeliveryContracts.navigation(p));
    PacketDistributor.sendToPlayer(p, new State(t));
  }

  @SubscribeEvent
  public static void login(PlayerEvent.PlayerLoggedInEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) sync(p);
  }

  @SubscribeEvent
  public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) sync(p);
  }

  @SubscribeEvent
  public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) sync(p);
  }

  public static CompoundTag inspect(ServerPlayer player, int id) {
    var t = new CompoundTag();
    t.putBoolean("installed", installed(player));
    t.putInt("range", range(player));
    t.put("contract", dev.cybernetics2077.city.DeliveryContracts.navigation(player));
    if (!installed(player) || !player.isAlive() || player.containerMenu != player.inventoryMenu)
      return t;
    var entity = player.serverLevel().getEntity(id);
    if (!(entity instanceof LivingEntity target)
        || !target.isAlive()
        || target == player
        || target.isSpectator()
        || target.isInvisibleTo(player)
        || !player.hasLineOfSight(target)) return t;
    var eye = player.getEyePosition();
    if (target
        .getBoundingBox()
        .inflate(.35)
        .clip(eye, eye.add(player.getLookAngle().scale(range(player))))
        .isEmpty()) return t;
    t.putInt("id", target.getId());
    t.putString("name", target.getName().getString());
    t.putString(
        "type",
        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(target.getType())
            .toString());
    t.putFloat("hp", target.getHealth());
    t.putFloat("maxHp", target.getMaxHealth());
    t.putFloat("absorption", target.getAbsorptionAmount());
    t.putInt("armor", target.getArmorValue());
    t.putDouble(
        "attack",
        target.getAttribute(Attributes.ATTACK_DAMAGE) == null
            ? 0
            : target.getAttributeValue(Attributes.ATTACK_DAMAGE));
    var effects = new ListTag();
    for (var effect : target.getActiveEffects())
      if (effect.isVisible()) {
        effects.add(StringTag.valueOf(effect.getDescriptionId()));
        if (effects.size() >= 8) break;
      }
    t.put("effects", effects);
    if (target instanceof Mob mob && dev.cybernetics2077.mob.EnhancedMobs.enhanced(mob)) {
      var loadout = dev.cybernetics2077.mob.EnhancedMobs.loadout(mob);
      t.putInt("implants", loadout.count());
      t.putDouble("averageTier", loadout.averageTier());
    }
    return t;
  }

  private static void request(ServerPlayer p, int id) {
    long now = p.serverLevel().getGameTime();
    if (now - LAST.getOrDefault(p, Long.MIN_VALUE / 2) < 4) return;
    LAST.put(p, now);
    PacketDistributor.sendToPlayer(p, new State(inspect(p, id)));
  }

  private ScannerNetwork() {}
}
