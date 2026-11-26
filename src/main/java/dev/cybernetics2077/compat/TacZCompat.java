package dev.cybernetics2077.compat;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

/** Optional API bridge: no TaCZ classes are resolved when the mod is absent. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "cybernetics2077")
public final class TacZCompat {
  public static final String DATA = "cybernetics2077_tacz";
  private static final Map<String, Method> METHODS = new ConcurrentHashMap<>();
  private static Class<?> gunClass, bulletClass;
  private static boolean loaded;
  public static java.util.function.Predicate<LivingEntity> clientBlocked = e -> false;
  public static java.util.function.ToDoubleFunction<LivingEntity> clientRecoil = e -> 1;

  public static void init() {
    if (!ModList.get().isLoaded("tacz")) return;
    try {
      gunClass =
          Class.forName("com.tacz.guns.api.item.IGun", false, TacZCompat.class.getClassLoader());
      bulletClass =
          Class.forName(
              "com.tacz.guns.entity.EntityKineticBullet", false, TacZCompat.class.getClassLoader());
      listen("common.GunShootEvent", TacZCompat::fire);
      listen("common.GunFireEvent", TacZCompat::fire);
      listen("common.GunReloadEvent", TacZCompat::reload);
      listen(
          "common.GunDrawEvent",
          e -> {
            if (call(e, "getEntity") instanceof ServerPlayer p) HandsRuntime.primeWrap(p);
          });
      listen("common.EntityHurtByGunEvent$Pre", TacZCompat::hurt);
      listen("common.EntityHurtByGunEvent$Post", TacZCompat::hit);
      listen("common.EntityKillByGunEvent", TacZCompat::hit);
      listen("server.AmmoHitBlockEvent", TacZCompat::bounce);
      loaded = true;
      com.mojang.logging.LogUtils.getLogger()
          .info("Cybernetics 2077: TaCZ gun integration enabled");
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(
          "Unsupported TaCZ API; update Cybernetics 2077 or use its documented TaCZ version", ex);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void listen(String name, Consumer<Object> handler)
      throws ReflectiveOperationException {
    Class type = Class.forName("com.tacz.guns.api.event." + name);
    NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, type, (Consumer) handler);
  }

  private static Object call(Object owner, String name, Object... args) {
    if (owner == null) return null;
    String key = owner.getClass().getName() + "#" + name + "/" + args.length;
    try {
      Method method =
          METHODS.computeIfAbsent(
              key,
              k ->
                  Arrays.stream(owner.getClass().getMethods())
                      .filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length)
                      .findFirst()
                      .orElseThrow());
      return method.invoke(owner, args);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("TaCZ bridge failed at " + key, ex);
    }
  }

  public static boolean gun(ItemStack stack) {
    return loaded && gunClass.isInstance(stack.getItem());
  }

  public static boolean bullet(Entity entity) {
    return loaded && bulletClass.isInstance(entity);
  }

  public static boolean blocked(LivingEntity entity) {
    return entity instanceof ServerPlayer p
        ? MantisHands.occupied(p) || AugmentRuntime.berserk(p)
        : entity.level().isClientSide && clientBlocked.test(entity);
  }

  private static LivingEntity shooter(Object event) {
    // Fire/Shoot use getShooter; Reload/Draw use getEntity.
    Object value =
        call(
            event,
            event.getClass().getSimpleName().equals("GunReloadEvent") ? "getEntity" : "getShooter");
    return value instanceof LivingEntity living ? living : null;
  }

  private static void cancel(Object event) {
    ((ICancellableEvent) event).setCanceled(true);
  }

  private static void fire(Object event) {
    LivingEntity shooter = shooter(event);
    if (shooter == null) return;
    if (blocked(shooter)) {
      cancel(event);
      return;
    }
  }

  private static void reload(Object event) {
    LivingEntity shooter = shooter(event);
    if (shooter != null && blocked(shooter)) cancel(event);
  }

  private static void hurt(Object event) {
    if (!(call(event, "getAttacker") instanceof ServerPlayer player)
        || !(call(event, "getHurtEntity") instanceof LivingEntity target)) return;
    if (!CyberwareRuntime.canAttack(player, target)) {
      cancel(event);
      return;
    }
    float base = ((Number) call(event, "getBaseAmount")).floatValue();
    if (base <= 0) return;
    if (player.getRandom().nextFloat() < AugmentExpansion.weaponCritChance(player, target, false)) {
      base *= AugmentExpansion.critMultiplier(player);
      player
          .serverLevel()
          .sendParticles(
              net.minecraft.core.particles.ParticleTypes.CRIT,
              target.getX(),
              target.getEyeY(),
              target.getZ(),
              8,
              .2,
              .3,
              .2,
              .03);
    }
    if (Boolean.TRUE.equals(call(event, "isHeadShot"))
        && Sandevistan.active(player)
        && Sandevistan.model(player).iconic())
      call(
          event,
          "setHeadshotMultiplier",
          ((Number) call(event, "getHeadshotMultiplier")).floatValue()
              * Sandevistan.model(player).headshotMultiplier());
    Entity bullet = (Entity) call(event, "getBullet");
    if (bullet != null && bullet.getPersistentData().getCompound(DATA).getBoolean("bounced"))
      base *= 1 + .05f * AugmentRuntime.tier(player, BALLISTIC_COPROCESSOR);
    if (bullet != null) {
      var data = bullet.getPersistentData().getCompound(DATA);
      data.putUUID("hitTarget", target.getUUID());
      data.putFloat("beforeHit", target.getHealth() + target.getAbsorptionAmount());
      bullet.getPersistentData().put(DATA, data);
    }
    call(event, "setBaseAmount", base);
  }

  private static void hit(Object event) {
    if (call(event, "getAttacker") instanceof ServerPlayer player
        && call(
                event,
                event.getClass().getSimpleName().equals("EntityKillByGunEvent")
                    ? "getKilledEntity"
                    : "getHurtEntity")
            instanceof LivingEntity target) {
      Entity bullet = (Entity) call(event, "getBullet");
      if (bullet == null) return;
      var data = bullet.getPersistentData().getCompound(DATA);
      if (data.hasUUID("hitTarget")
          && data.getUUID("hitTarget").equals(target.getUUID())
          && data.getFloat("beforeHit") > target.getHealth() + target.getAbsorptionAmount())
        HandsRuntime.discharge(player, target, AugmentRuntime.tier(player, MICROGENERATOR));
      data.remove("hitTarget");
    }
  }

  @SubscribeEvent
  public static void entityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre e) {
    tick(e.getEntity());
  }

  public static void tick(Entity entity) {
    if (!bullet(entity)
        || !(entity instanceof Projectile projectile)
        || !(entity.level() instanceof ServerLevel level)
        || !(projectile.getOwner() instanceof ServerPlayer player)) return;
    var tag = entity.getPersistentData().getCompound(DATA);
    if (!entity.getPersistentData().contains(DATA)) {
      tag = new net.minecraft.nbt.CompoundTag();
      entity.getPersistentData().put(DATA, tag);
    }
    if (tag.getBoolean("pending")) {
      var point = new Vec3(tag.getDouble("px"), tag.getDouble("py"), tag.getDouble("pz"));
      entity.setPos(point);
      entity.setDeltaMovement(tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz"));
      tag.remove("pending");
      sync(entity);
    }
    if (tag.getBoolean("bounced")) return;
    LivingEntity target = HandsRuntime.locked(player);
    if (target == null
        || entity.tickCount > 40
        || entity.distanceToSqr(target) > 64 * 64
        || !player.hasLineOfSight(target)
        || !HandsRuntime.clear(
            level, entity.position(), target.getBoundingBox().getCenter(), entity)) return;
    var v = entity.getDeltaMovement();
    var d = target.getBoundingBox().getCenter().subtract(entity.position());
    var turn =
        HandsRules.turn(
            v.x, v.y, v.z, d.x, d.y, d.z, .035 + .012 * AugmentRuntime.tier(player, SMART_LINK));
    entity.setDeltaMovement(turn.x(), turn.y(), turn.z());
    if (entity.tickCount % 2 == 0) sync(entity);
  }

  private static void bounce(Object event) {
    if (!(call(event, "getAmmo") instanceof Projectile bullet)
        || !(bullet.level() instanceof ServerLevel level)
        || !(bullet.getOwner() instanceof ServerPlayer player)) return;
    int tier = AugmentRuntime.tier(player, BALLISTIC_COPROCESSOR);
    var tag = bullet.getPersistentData().getCompound(DATA);
    if (tier == 0
        || tag.getBoolean("bounced")
        || explosive(bullet)
        || bullet.getDeltaMovement().lengthSqr() < .04) return;
    BlockHitResult hit = (BlockHitResult) call(event, "getHitResult");
    if (hit.getType() == HitResult.Type.MISS) return;
    var n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
    var v = bullet.getDeltaMovement();
    var reflected = HandsRules.reflect(v.x, v.y, v.z, n.x, n.y, n.z);
    Vec3 velocity =
        new Vec3(reflected.x(), reflected.y(), reflected.z()).scale(HandsRules.bounceSpeed(tier));
    var point = hit.getLocation().add(n.scale(.08));
    tag.putBoolean("bounced", true);
    tag.putBoolean("pending", true);
    tag.putDouble("px", point.x);
    tag.putDouble("py", point.y);
    tag.putDouble("pz", point.z);
    tag.putDouble("vx", velocity.x);
    tag.putDouble("vy", velocity.y);
    tag.putDouble("vz", velocity.z);
    bullet.getPersistentData().put(DATA, tag);
    bullet.setDeltaMovement(Vec3.ZERO);
    cancel(event);
    level.sendParticles(
        net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
        point.x,
        point.y,
        point.z,
        8,
        .05,
        .05,
        .05,
        .015);
  }

  private static boolean explosive(Entity bullet) {
    try {
      Field field = bulletClass.getDeclaredField("explosion");
      field.setAccessible(true);
      return field.getBoolean(bullet);
    } catch (ReflectiveOperationException ex) {
      return true;
    } // Unknown explosive metadata must never enable a ricochet.
  }

  private static void sync(Entity entity) {
    if (!(entity.level() instanceof ServerLevel level)) return;
    var v = entity.getDeltaMovement();
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
        entity,
        new CyberwareNetwork.Ballistics(
            entity.getId(),
            entity.getUUID(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            v.x,
            v.y,
            v.z));
  }

  public static long interval(LivingEntity shooter, long nativeInterval) {
    double pace = Sandevistan.weaponPace(shooter);
    return Math.max(1, Math.round(nativeInterval / Math.max(.05, pace)));
  }

  public static boolean aiming(LivingEntity shooter) {
    if (!gun(shooter.getMainHandItem())) return false;
    try {
      Class<?> api = Class.forName("com.tacz.guns.api.entity.IGunOperator");
      Object operator = api.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, shooter);
      return Boolean.TRUE.equals(call(operator, "getSynIsAiming"));
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static double recoil(LivingEntity player) {
    if (player == null) return 1;
    if (player instanceof ServerPlayer p)
      return recoil(
          AugmentRuntime.tier(p, SHOCK_ABSORBER),
          AugmentRuntime.tier(p, IMMOVABLE_FORCE),
          p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() < .005);
    return clientRecoil.applyAsDouble(player);
  }

  public static double recoil(int shock, int force, boolean stationary) {
    return Math.max(.15, (1 - .08 * shock) * (stationary ? 1 - .07 * force : 1));
  }

  public static List<String> details(Augment augment, int tier) {
    if (!loaded) return List.of();
    return switch (augment) {
      case SMART_LINK -> List.of("TaCZ: guided bullets follow your scanner lock.");
      case BALLISTIC_COPROCESSOR -> List.of("TaCZ: one ricochet; explosive rounds excluded.");
      case MICROGENERATOR -> List.of("TaCZ: landed gunshots trigger the discharge.");
      case HANDLE_WRAP -> List.of("TaCZ: drawing a gun primes the critical-hit bonus.");
      case SHOCK_ABSORBER -> List.of("TaCZ: " + (8 * tier) + "% less camera recoil; steadier aim.");
      case IMMOVABLE_FORCE ->
          List.of("TaCZ: " + (7 * tier) + "% less recoil while grounded and still.");
      default -> List.of();
    };
  }

  private TacZCompat() {}
}
