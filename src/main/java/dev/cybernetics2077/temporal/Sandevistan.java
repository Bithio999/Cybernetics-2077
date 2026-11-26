package dev.cybernetics2077.temporal;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.CyberwareRuntime;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Server-authoritative activation and shared encounter clocks; never changes the server tick rate.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class Sandevistan {
  public record View(int tier, long end, double pace, boolean pvp, boolean apogee, boolean david) {
    public View(int tier, long end, double pace, boolean pvp, boolean apogee) {
      this(tier, end, pace, pvp, apogee, false);
    }

    public View(int tier, long end, double pace, boolean pvp) {
      this(tier, end, pace, pvp, false);
    }
  }

  public static java.util.function.Function<Player, View> client = p -> new View(0, 0, 1, false);

  private record Field(Player owner, double strength, boolean pvp) {}

  private static final ThreadLocal<Map<Level, List<Field>>> FIELDS =
      ThreadLocal.withInitial(WeakHashMap::new);
  private static final ThreadLocal<Map<Level, Long>> CLOCKS =
      ThreadLocal.withInitial(WeakHashMap::new);
  private static final ThreadLocal<Map<Entity, TemporalRules.Budget>> BUDGETS =
      ThreadLocal.withInitial(WeakHashMap::new);

  public static int tier(Player p) {
    return Math.max(
        Math.max(
            AugmentRuntime.tier(p, Augment.DYNALAR_SANDEVISTAN),
            AugmentRuntime.tier(p, Augment.DAVIDS_SANDEVISTAN)),
        AugmentRuntime.tier(p, Augment.MILITECH_APOGEE));
  }

  public static SandevistanModel model(Player p) {
    return SandevistanModel.of(
        p instanceof ServerPlayer
            ? AugmentRuntime.tier(p, Augment.MILITECH_APOGEE) > 0
            : client.apply(p).apogee,
        p instanceof ServerPlayer
            ? AugmentRuntime.tier(p, Augment.DAVIDS_SANDEVISTAN) > 0
            : client.apply(p).david);
  }

  public static boolean active(Player p) {
    if (!p.isAlive() || p.isSpectator()) return false;
    if (p instanceof ServerPlayer sp)
      return tier(p) > 0 && AugmentRuntime.state(p).sandyEnd > AugmentRuntime.now(sp);
    var v = client.apply(p);
    return v.tier > 0 && v.end > p.level().getGameTime();
  }

  public static double strength(Player p) {
    if (!active(p)) return 1;
    int t = p instanceof ServerPlayer ? tier(p) : client.apply(p).tier;
    return TemporalRules.acceleration(model(p).slowing(t));
  }

  public static double charge(ServerPlayer p) {
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (s.sandyEnd > 0)
      return Math.clamp(
          (s.sandyEnd - now)
              / (20. * SandevistanModel.of(s.sandyApogee, s.sandyDavid).duration(s.sandyProfile)),
          0,
          1);
    return Math.clamp(
        s.sandyCharge
            + Math.max(0, now - s.sandyClock)
                / (20. * SandevistanModel.of(s.sandyApogee, s.sandyDavid).recharge(s.sandyProfile)),
        0,
        1);
  }

  public static void stop(ServerPlayer p, long when) {
    var s = AugmentRuntime.state(p);
    if (s.sandyEnd == 0) return;
    s.sandyCharge =
        Math.clamp(
            (s.sandyEnd - when)
                / (20. * SandevistanModel.of(s.sandyApogee, s.sandyDavid).duration(s.sandyProfile)),
            0,
            1);
    s.sandyClock = when;
    s.sandyEnd = 0;
    s.sandyRearm = when + 20;
    invalidate(p.level());
  }

  public static boolean toggle(ServerPlayer p) {
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (!CyberwareRuntime.usable(p) || p.isPassenger()) return false;
    int t = tier(p);
    if (t == 0) return false;
    if (active(p)) {
      stop(p, now);
      p.level()
          .playSound(
              null,
              p.blockPosition(),
              SoundEvents.BEACON_DEACTIVATE,
              SoundSource.PLAYERS,
              .6f,
              1.5f);
    } else {
      if (now < s.sandyRearm || charge(p) + 1e-9 < model(p).minimumCharge(t)) {
        p.displayClientMessage(Component.translatable("cybernetics2077.sandy.recharging"), true);
        return false;
      }
      double available = charge(p);
      s.sandyProfile = t;
      s.sandyApogee = model(p) == SandevistanModel.APOGEE;
      s.sandyDavid = model(p) == SandevistanModel.DAVID;
      s.sandyCharge = available;
      s.sandyClock = now;
      s.sandyEnd = now + Math.max(1, Math.round(available * model(p).duration(t) * 20));
      invalidate(p.level());
      p.level()
          .playSound(
              null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, .7f, 1.6f);
    }
    tick(p);
    CyberwareRuntime.broadcast(p);
    return true;
  }

  public static void invalidate(Level level) {
    CLOCKS.get().remove(level);
  }

  private static List<Field> fields(Level level) {
    long now = level.getGameTime();
    if (!Objects.equals(CLOCKS.get().get(level), now)) {
      var list = new ArrayList<Field>();
      for (var p : level.players())
        if (active(p))
          list.add(
              new Field(
                  p,
                  strength(p),
                  p instanceof ServerPlayer sp ? sp.server.isPvpAllowed() : client.apply(p).pvp));
      FIELDS.get().put(level, List.copyOf(list));
      CLOCKS.get().put(level, now);
    }
    return FIELDS.get().getOrDefault(level, List.of());
  }

  public static double pace(Entity entity) {
    // A firearm projectile stays on the world clock, even when fired by an accelerated owner.
    return pace(entity, !dev.cybernetics2077.compat.TacZCompat.bullet(entity));
  }

  /** Gun mechanisms do not inherit their wielder's personal acceleration. */
  public static double weaponPace(LivingEntity entity) {
    return Math.min(1, pace(entity, false));
  }

  private static double pace(Entity entity, boolean personalClock) {
    if (!entity.isAlive() || entity.isSpectator()) return 1;
    double personal =
        !personalClock
            ? 1
            : entity instanceof Player p
                ? strength(p)
                : entity instanceof Projectile projectile
                        && projectile.getOwner() instanceof Player p
                    ? strength(p)
                    : 1;
    double strongest = entity instanceof Player p ? strength(p) : 1;
    for (var field : fields(entity.level())) {
      var owner = field.owner;
      if (owner.level() != entity.level()) continue;
      if (owner.distanceToSqr(entity) > TemporalRules.RADIUS * TemporalRules.RADIUS) continue;
      if (entity instanceof Player other && other != owner) {
        if (other.isCreative() || owner.isAlliedTo(other) || !field.pvp) continue;
        if (owner instanceof ServerPlayer sp && !sp.canHarmPlayer(other)) continue;
      } else if (entity instanceof Projectile projectile
          && projectile.getOwner() instanceof Player shooter
          && shooter != owner) {
        if (!field.pvp
            || owner.isAlliedTo(shooter)
            || owner instanceof ServerPlayer sp && !sp.canHarmPlayer(shooter)) continue;
      } else if (entity instanceof Mob && (owner.isAlliedTo(entity) || entity.isAlliedTo(owner)))
        continue;
      strongest = Math.max(strongest, field.strength);
    }
    return TemporalRules.pace(personal, strongest);
  }

  public static java.util.List<net.minecraft.world.phys.Vec3> fieldOrigins(Level level) {
    return fields(level).stream().map(f -> f.owner.position()).toList();
  }

  /** Environmental updates have no personal acceleration and use the strongest local field. */
  public static double environmentPace(Level level, double x, double y, double z, boolean blend) {
    double result = 1;
    for (var field : fields(level)) {
      double distance = field.owner.distanceToSqr(x, y, z);
      if (distance >= TemporalRules.RADIUS * TemporalRules.RADIUS) continue;
      double pace = Math.min(1, TemporalRules.pace(1, field.strength));
      double edge = blend ? Math.clamp((TemporalRules.RADIUS - Math.sqrt(distance)) / 6, 0, 1) : 1;
      edge = edge * edge * (3 - 2 * edge);
      result = Math.min(result, 1 - (1 - pace) * edge);
    }
    return result;
  }

  public static double playerPace(Player p) {
    return p instanceof ServerPlayer ? pace(p) : client.apply(p).pace;
  }

  private static void modifier(
      ServerPlayer p, net.minecraft.core.Holder<Attribute> attr, double factor) {
    var a = p.getAttribute(attr);
    if (a == null) return;
    var id = Cybernetics2077.id("temporal_pace");
    var old = a.getModifier(id);
    if (old != null && Math.abs(old.amount() - (factor - 1)) < 1e-6) return;
    a.removeModifier(id);
    if (Math.abs(factor - 1) > .00001)
      a.addTransientModifier(
          new AttributeModifier(id, factor - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
  }

  public static void tick(ServerPlayer p) {
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (s.sandyEnd > 0
        && (tier(p) != s.sandyProfile
            || model(p) != SandevistanModel.of(s.sandyApogee, s.sandyDavid)
            || !p.isAlive()
            || p.isSpectator()
            || now >= s.sandyEnd)) stop(p, Math.min(now, s.sandyEnd));
    double pace = pace(p);
    modifier(p, Attributes.MOVEMENT_SPEED, pace);
    modifier(p, Attributes.ATTACK_SPEED, pace);
    boolean active = active(p);
    if (active != s.wasSandy || Math.abs(pace - s.lastSandyPace) > .001) {
      s.wasSandy = active;
      s.lastSandyPace = pace;
      CyberwareRuntime.broadcast(p);
    }
  }

  public static void snapshot(ServerPlayer p, CompoundTag tag) {
    var s = AugmentRuntime.state(p);
    tag.putInt("sandyTier", tier(p));
    tag.putBoolean("sandyIconic", model(p).iconic());
    tag.putInt("sandyModel", model(p).ordinal());
    tag.putLong("sandyEnd", active(p) ? s.sandyEnd : 0);
    tag.putDouble("temporalPace", pace(p));
    tag.putBoolean("sandyPvp", p.server.isPvpAllowed());
    // Absolute reference allows a client-side charge bar without a packet every tick.
    tag.putDouble("sandyCharge", s.sandyEnd > 0 ? 0 : s.sandyCharge);
    tag.putLong("sandyClock", s.sandyClock);
    tag.putInt("sandyProfile", s.sandyProfile);
    tag.putBoolean("sandyApogee", s.sandyApogee);
    tag.putBoolean("sandyDavid", s.sandyDavid);
    tag.putLong("sandyRearm", s.sandyRearm);
  }

  public static boolean controlled(Entity e) {
    return (e instanceof Mob
            || e instanceof Projectile
            || e instanceof net.minecraft.world.entity.item.ItemEntity
            || e instanceof net.minecraft.world.entity.ExperienceOrb
            || e instanceof net.minecraft.world.entity.item.FallingBlockEntity
            || e instanceof net.minecraft.world.entity.item.PrimedTnt
            || e instanceof net.minecraft.world.entity.vehicle.Boat
            || e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart)
        && e.isAlive()
        && !e.isPassenger()
        && !e.isVehicle();
  }

  /** Native projectile collision and mob AI run at the same fractional rate as their physics. */
  public static void tickEntity(Entity entity, Runnable vanilla) {
    if (!controlled(entity)) {
      BUDGETS.get().remove(entity);
      vanilla.run();
      return;
    }
    double pace = pace(entity);
    if (Math.abs(pace - 1) < 1e-6) {
      BUDGETS.get().remove(entity);
      vanilla.run();
      return;
    }
    var budget = BUDGETS.get().computeIfAbsent(entity, e -> new TemporalRules.Budget());
    int ticks = budget.advance(pace);
    int inv = entity instanceof LivingEntity living ? living.invulnerableTime : 0;
    for (int i = 0; i < ticks && !entity.isRemoved(); i++) vanilla.run();
    // Slow motion must not turn vanilla hurt protection into several real seconds of immunity.
    if (entity instanceof LivingEntity living && inv > 0 && living.invulnerableTime <= inv)
      living.invulnerableTime = Math.max(0, inv - 1);
  }

  public static float renderPartial(Entity e, float partial) {
    var b = BUDGETS.get().get(e);
    return b == null ? partial : TemporalRules.partial(b.phase, pace(e), partial);
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) stop(p, AugmentRuntime.now(p));
  }

  @SubscribeEvent
  public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      stop(p, AugmentRuntime.now(p));
      invalidate(p.level());
    }
  }

  @SubscribeEvent
  public static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) stop(p, AugmentRuntime.now(p));
  }

  @SubscribeEvent
  public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload e) {
    FIELDS.get().remove(e.getLevel());
    CLOCKS.get().remove(e.getLevel());
    BUDGETS.get().keySet().removeIf(entity -> entity.level() == e.getLevel());
  }

  @SubscribeEvent
  public static void hurt(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
    if (e.getSource().getEntity() instanceof ServerPlayer p
        && active(p)
        && e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK))
      e.setInvulnerabilityTicks(
          Math.min(
              e.getContainer().getPostAttackInvulnerabilityTicks(),
              (int) Math.ceil(20 / playerPace(p))));
  }

  private Sandevistan() {}
}
