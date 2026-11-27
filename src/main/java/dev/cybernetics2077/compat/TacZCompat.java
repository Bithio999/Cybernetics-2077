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
  private static Method blockClip;
  private static Field explosiveField;
  private static final Map<Entity, FlightStep> FLIGHT = new WeakHashMap<>();

  private static final class FlightStep {
    final Vec3 start;
    Vec3 hit;

    FlightStep(Vec3 start) {
      this.start = start;
    }
  }

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
      blockClip =
          Class.forName("com.tacz.guns.util.block.BlockRayTrace")
              .getMethod(
                  "rayTraceBlocks",
                  net.minecraft.world.level.Level.class,
                  net.minecraft.world.level.ClipContext.class);
      explosiveField = bulletClass.getDeclaredField("explosion");
      explosiveField.setAccessible(true);
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
    return !entity.level().isClientSide
            && (ExpansionRuntime.jammed(entity)
                || entity instanceof Mob m && dev.cybernetics2077.mob.MobImplants.berserk(m))
        || (entity instanceof ServerPlayer p
            ? MantisHands.occupied(p) || AugmentRuntime.berserk(p)
            : entity.level().isClientSide && clientBlocked.test(entity));
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
    if (!(call(event, "getAttacker") instanceof LivingEntity player)
        || !(call(event, "getHurtEntity") instanceof LivingEntity target)) return;
    if (!NeuralActors.canAttack(player, target)) {
      cancel(event);
      return;
    }
    float base = ((Number) call(event, "getBaseAmount")).floatValue();
    if (base <= 0) return;
    float crit =
        player instanceof ServerPlayer p
            ? AugmentExpansion.weaponCritChance(p, target, false)
            : Math.min(
                    .5f,
                    AugmentRuntime.tier(player, VISUAL_CORTEX_SUPPORT)
                        * .06f
                        * (float) Math.clamp(player.distanceTo(target) / 32., 0, 1))
                + ExpansionRuntime.critChance(player, target);
    if (player.getRandom().nextFloat() < crit) {
      base *= player instanceof ServerPlayer p ? AugmentExpansion.critMultiplier(p) : 1.5f;
      ((ServerLevel) player.level())
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
    base *=
        ExpansionRuntime.attackBonus(
            player,
            target,
            true,
            Boolean.TRUE.equals(call(event, "isHeadShot")),
            aiming(player)
                && player.level().getGameTime() - ExpansionRuntime.extra(player).getLong("aimStart")
                    >= 10);
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
    if (call(event, "getAttacker") instanceof LivingEntity player
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
          && data.getFloat("beforeHit") > target.getHealth() + target.getAbsorptionAmount()) {
        if (player instanceof ServerPlayer p)
          HandsRuntime.discharge(p, target, AugmentRuntime.tier(player, MICROGENERATOR));
        else if (player instanceof Mob m) dev.cybernetics2077.mob.MobImplants.discharge(m, target);
        if (aiming(player)
            && player.level().getGameTime() - ExpansionRuntime.extra(player).getLong("aimStart")
                >= 10) ExpansionRuntime.precisionHit(player, target);
      }
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
        || !(projectile.getOwner() instanceof LivingEntity player)) return;
    var tag = entity.getPersistentData().getCompound(DATA);
    if (!entity.getPersistentData().contains(DATA)) {
      tag = new net.minecraft.nbt.CompoundTag();
      entity.getPersistentData().put(DATA, tag);
    }
    completeBounce(entity);
    LivingEntity target =
        tag.getBoolean("bounced")
            ? null
            : player instanceof ServerPlayer p
                ? HandsRuntime.locked(p)
                : player instanceof Mob m
                    ? dev.cybernetics2077.mob.MobImplants.smartTarget(m)
                    : null;
    if (target != null
        && BulletTime.age(entity) <= 40
        && entity.distanceToSqr(target) <= 64 * 64
        && player.hasLineOfSight(target)
        && HandsRuntime.clear(
            level, entity.position(), target.getBoundingBox().getCenter(), entity)) {
      var v = entity.getDeltaMovement();
      var d = target.getBoundingBox().getCenter().subtract(entity.position());
      var movement = target.position().subtract(new Vec3(target.xo, target.yo, target.zo));
      var turn =
          HandsRules.guide(
              v.x,
              v.y,
              v.z,
              d.x,
              d.y,
              d.z,
              movement.x,
              movement.y,
              movement.z,
              HandsRuntime.smartTier(player),
              BulletTime.eligible(entity) ? Math.min(1, Sandevistan.pace(entity)) : 1);
      entity.setDeltaMovement(turn.x(), turn.y(), turn.z());
      tag.putBoolean("controlled", true);
    }
    if (tag.getBoolean("controlled")) sync(entity);
  }

  /** Capture only the first two physical steps after a confirmed bounce. */
  public static void beginFlight(Entity entity) {
    if (!(entity.level() instanceof ServerLevel)) return;
    var tag = entity.getPersistentData().getCompound(DATA);
    if (tag.getBoolean("bounced")
        && tag.getInt("traceSteps") < 2
        && entity.getDeltaMovement().lengthSqr() > .0001)
      FLIGHT.put(entity, new FlightStep(entity.position()));
  }

  public static void flightHit(Entity entity, Vec3 point) {
    if (!(entity.level() instanceof ServerLevel)) return;
    BulletTime.hit(entity, point);
    var step = FLIGHT.get(entity);
    if (step != null) step.hit = point;
  }

  /** Runs before removal reaches clients; endpoints come from real collision results. */
  public static void finishFlight(Entity entity) {
    if (!(entity.level() instanceof ServerLevel)) return;
    var step = FLIGHT.remove(entity);
    if (step == null) return;
    Vec3 end = entity.isRemoved() && step.hit != null ? step.hit : BulletTime.end(entity);
    var tag = entity.getPersistentData().getCompound(DATA);
    tag.putInt("traceSteps", tag.getInt("traceSteps") + 1);
    rebound(entity, step.start, end);
  }

  private static void rebound(Entity entity, Vec3 from, Vec3 to) {
    var level = (ServerLevel) entity.level();
    // The short-lived visual must reach observers even when the native entity is already removed.
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(
        level,
        null,
        from.x,
        from.y,
        from.z,
        128,
        new CyberwareNetwork.Rebound(entity.getUUID(), from.x, from.y, from.z, to.x, to.y, to.z));
  }

  /** Same read-only collision query TaCZ uses, including its pass-through block settings. */
  public static BlockHitResult clip(
      net.minecraft.world.level.Level level, Vec3 from, Vec3 to, Entity context) {
    var query =
        new net.minecraft.world.level.ClipContext(
            from,
            to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            // Tweaks treats a bullet collision context as a live shot and applies damage here.
            // Geometry/preview queries must never identify themselves as a live bullet sweep.
            bullet(context) || context == null
                ? net.minecraft.world.phys.shapes.CollisionContext.empty()
                : net.minecraft.world.phys.shapes.CollisionContext.of(context));
    if (!loaded) return level.clip(query);
    try {
      return (BlockHitResult) blockClip.invoke(null, level, query);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("TaCZ collision query failed", ex);
    }
  }

  public static boolean kinetic(Entity entity) {
    return bullet(entity) && !explosive(entity);
  }

  public static boolean completeBounce(Entity entity) {
    if (!(entity.level() instanceof ServerLevel)) return false;
    var tag = entity.getPersistentData().getCompound(DATA);
    if (!tag.getBoolean("pending")) return false;
    entity.setPos(tag.getDouble("px"), tag.getDouble("py"), tag.getDouble("pz"));
    entity.setDeltaMovement(tag.getDouble("vx"), tag.getDouble("vy"), tag.getDouble("vz"));
    tag.remove("pending");
    BulletTime.bounce(entity);
    tag.putBoolean("controlled", true);
    sync(entity);
    rebound(entity, entity.position(), entity.position());
    return true;
  }

  private static void bounce(Object event) {
    if (!(call(event, "getAmmo") instanceof Projectile bullet)
        || !(bullet.level() instanceof ServerLevel level)
        || !(bullet.getOwner() instanceof LivingEntity player)) return;
    BlockHitResult hit = (BlockHitResult) call(event, "getHitResult");
    int tier = AugmentRuntime.tier(player, BALLISTIC_COPROCESSOR);
    var tag = bullet.getPersistentData().getCompound(DATA);
    // Some collision pipelines dispatch the same impact again before onBulletTick returns.
    // Keep suppressing that impact until completeBounce has moved the round out of the wall.
    if (tag.getBoolean("pending")) {
      cancel(event);
      return;
    }
    if (tier == 0
        || tag.getBoolean("bounced")
        || explosive(bullet)
        || bullet.getDeltaMovement().lengthSqr() < .04) return;
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
      return explosiveField.getBoolean(bullet);
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
            v.z,
            level.getGameTime()));
  }

  public static long interval(LivingEntity shooter, long nativeInterval) {
    double pace = Sandevistan.weaponPace(shooter) * ExpansionRuntime.gunRate(shooter);
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

  public record BallisticProfile(double speed, double gravity, double friction, int lifeTicks) {}

  private record CachedProfile(long tick, ItemStack stack, BallisticProfile profile) {}

  private static final Map<LivingEntity, CachedProfile> PROFILES = new WeakHashMap<>();

  /** Native gun data and attachment properties, cached for the current simulation tick. */
  public static BallisticProfile ballistics(LivingEntity shooter) {
    ItemStack stack = shooter.getMainHandItem();
    if (!gun(stack)) return null;
    long tick = shooter.level().getGameTime();
    var old = PROFILES.get(shooter);
    if (old != null && old.tick == tick && old.stack == stack) return old.profile;
    BallisticProfile result = null;
    try {
      var api = Class.forName("com.tacz.guns.api.TimelessAPI");
      var index =
          (Optional<?>)
              api.getMethod("getCommonGunIndex", net.minecraft.resources.ResourceLocation.class)
                  .invoke(null, call(stack.getItem(), "getGunId", stack));
      if (index.isPresent()) {
        Object data = call(index.get(), "getGunData"), bullet = call(data, "getBulletData");
        Object explosion = call(bullet, "getExplosionData");
        if (explosion == null || !Boolean.TRUE.equals(call(explosion, "isExplode"))) {
          var operatorApi = Class.forName("com.tacz.guns.api.entity.IGunOperator");
          Object operator =
              operatorApi.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, shooter);
          Object cache = call(operator, "getCacheProperty");
          if (cache == null) {
            cache =
                Class.forName("com.tacz.guns.resource.modifier.AttachmentCacheProperty")
                    .getConstructor()
                    .newInstance();
            call(cache, "eval", stack, data);
          }
          // Select the String overload explicitly; the GunProperty overload has identical arity.
          Object speed =
              cache.getClass().getMethod("getCache", String.class).invoke(cache, "ammo_speed");
          float velocity =
              speed instanceof Number n
                  ? n.floatValue()
                  : ((Number) call(bullet, "getSpeed")).floatValue();
          Object holder = operatorApi.getMethod("getDataHolder").invoke(operator);
          Method modify =
              gunClass.getMethod(
                  "modifyProperty",
                  Class.forName("com.tacz.guns.entity.shooter.ShooterDataHolder"),
                  ItemStack.class,
                  LivingEntity.class,
                  String.class,
                  Class.class,
                  Object.class);
          velocity =
              ((Number)
                      modify.invoke(
                          stack.getItem(),
                          holder,
                          stack,
                          shooter,
                          "ammo_speed",
                          Float.class,
                          velocity))
                  .floatValue();
          double globalSpeed =
              ((Number)
                      call(
                          Class.forName("com.tacz.guns.config.common.AmmoConfig")
                              .getField("GLOBAL_BULLET_SPEED_MODIFIER")
                              .get(null),
                          "get"))
                  .doubleValue();
          float gravity =
              ((Number)
                      modify.invoke(
                          stack.getItem(),
                          holder,
                          stack,
                          shooter,
                          "bullet_gravity",
                          Float.class,
                          call(bullet, "getGravity")))
                  .floatValue();
          float friction =
              ((Number)
                      modify.invoke(
                          stack.getItem(),
                          holder,
                          stack,
                          shooter,
                          "bullet_friction",
                          Float.class,
                          call(bullet, "getFriction")))
                  .floatValue();
          float life = ((Number) call(bullet, "getLifeSecond")).floatValue();
          if (Float.isFinite(velocity)
              && velocity > 0
              && Float.isFinite(gravity)
              && Float.isFinite(friction))
            result =
                new BallisticProfile(
                    velocity * globalSpeed / 20,
                    Math.max(0, gravity),
                    Math.clamp(friction, 0, 1),
                    Math.clamp((int) (life * 20), 1, 200));
        }
      }
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("TaCZ preview properties unavailable", ex);
    }
    PROFILES.put(shooter, new CachedProfile(tick, stack, result));
    return result;
  }

  public static boolean reloading(LivingEntity player) {
    if (!gun(player.getMainHandItem())) return false;
    try {
      var api = Class.forName("com.tacz.guns.api.entity.IGunOperator");
      var op = api.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, player);
      return Boolean.TRUE.equals(
          call(call(call(op, "getSynReloadState"), "getStateType"), "isReloading"));
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

  public static double spread(LivingEntity shooter) {
    int tier =
        shooter instanceof ServerPlayer p
            ? AugmentRuntime.tier(p, IMMOVABLE_FORCE)
            : HandsRuntime.clientImmovable.applyAsInt(shooter);
    return HandsRules.spreadMultiplier(tier);
  }

  public static double recoil(int shock, int force, boolean stationary) {
    return Math.max(.15, (1 - .08 * shock) * (1 - .10 * force));
  }

  public static List<String> details(Augment augment, int tier) {
    return List.of();
  }

  private TacZCompat() {}
}
