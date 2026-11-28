package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class TacZGameTests {
  static Object call(Object o, String name, Object... args) throws Exception {
    return Arrays.stream(o.getClass().getMethods())
        .filter(m -> m.getName().equals(name) && m.getParameterCount() == args.length)
        .findFirst()
        .orElseThrow()
        .invoke(o, args);
  }

  static ServerPlayer player(GameTestHelper h) {
    var p =
        new net.neoforged.neoforge.common.util.FakePlayer(
            h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "CyberCompatTest"));
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 90, 2))));
    return p;
  }

  static void equip(ServerPlayer p, Augment a, int t, int socket) {
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            a.category.firstSlot() + socket, new ItemStack(Cybernetics2077.augment(a, t)));
  }

  static Projectile bullet(GameTestHelper h, ServerPlayer p) throws Exception {
    Class<?> type = Class.forName("com.tacz.guns.entity.EntityKineticBullet");
    var b =
        (Projectile)
            type.getConstructor(EntityType.class, net.minecraft.world.level.Level.class)
                .newInstance(type.getField("TYPE").get(null), h.getLevel());
    b.setOwner(p);
    b.setPos(p.position());
    b.setDeltaMovement(0, 0, 2);
    return b;
  }

  static Object event(String name, Object... args) throws Exception {
    var type = Class.forName("com.tacz.guns.api.event." + name);
    return Arrays.stream(type.getConstructors())
        .filter(c -> c.getParameterCount() == args.length)
        .findFirst()
        .orElseThrow()
        .newInstance(args);
  }

  static void post(Object e) {
    NeoForge.EVENT_BUS.post((net.neoforged.bus.api.Event) e);
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void roundBonusesPreserveNativeHeadshotsAndDoNotDoubleCrit(GameTestHelper h)
      throws Exception {
    var p = player(h);
    var target = h.spawn(EntityType.HUSK, new BlockPos(4, 90, 4));
    var b = bullet(h, p);
    var pre =
        event(
            "common.EntityHurtByGunEvent$Pre",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            true,
            2f,
            LogicalSide.SERVER);
    post(pre);
    h.assertTrue(
        ((Number) call(pre, "getBaseAmount")).floatValue() == 10f, "Bare gun damage changed");
    equip(p, Augment.MILITECH_APOGEE, 5, 0);
    Sandevistan.toggle(p);
    var boosted =
        event(
            "common.EntityHurtByGunEvent$Pre",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            true,
            2f,
            LogicalSide.SERVER);
    post(boosted);
    h.assertTrue(
        Math.abs(((Number) call(boosted, "getHeadshotMultiplier")).floatValue() - 2.4f) < .001,
        "Apogee replaced or lost native headshot multiplier");
    Sandevistan.stop(p, AugmentRuntime.now(p));
    equip(p, Augment.DAVIDS_SANDEVISTAN, 5, 0);
    AugmentRuntime.state(p).sandyRearm = 0;
    AugmentRuntime.state(p).sandyCharge = 1;
    Sandevistan.toggle(p);
    var david =
        event(
            "common.EntityHurtByGunEvent$Pre",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            true,
            2f,
            LogicalSide.SERVER);
    post(david);
    h.assertTrue(
        Math.abs(((Number) call(david, "getHeadshotMultiplier")).floatValue() - 2.5f) < .001,
        "David lost native TaCZ headshot multiplier");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void scannerLockGuidesRealBulletsAndRespectsWalls(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.SMART_LINK, 5, 0);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.FACE.firstSlot(), new ItemStack(Cybernetics2077.OPTICS.get()));
    var target = h.spawn(EntityType.HUSK, new BlockPos(6, 90, 8));
    for (int x = -1; x <= 7; x++)
      for (int y = 0; y <= 4; y++)
        for (int z = -1; z <= 9; z++)
          h.getLevel()
              .setBlock(p.blockPosition().offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    HandsRuntime.lock(p, target);
    h.assertTrue(
        HandsRuntime.locked(p) == target,
        "Native target did not lock: smart="
            + AugmentRuntime.tier(p, Augment.SMART_LINK)
            + " sight="
            + p.hasLineOfSight(target)
            + " range="
            + p.distanceTo(target));
    var b = bullet(h, p);
    TacZCompat.tick(b);
    h.assertTrue(b.getDeltaMovement().x > 0, "Smart Link did not guide native bullet");
    var v = b.getDeltaMovement();
    h.getLevel().setBlock(p.blockPosition().offset(1, 0, 1), Blocks.STONE.defaultBlockState(), 3);
    target.setPos(p.position().add(2, 0, 2));
    h.getLevel().setBlock(p.blockPosition().offset(1, 1, 1), Blocks.STONE.defaultBlockState(), 3);
    TacZCompat.tick(b);
    h.assertTrue(b.getDeltaMovement().equals(v), "Guidance bent through solid terrain");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void kineticRicochetOccursOnceAndExplosivesKeepNativeImpact(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.BALLISTIC_COPROCESSOR, 5, 0);
    var b = bullet(h, p);
    var hit =
        new BlockHitResult(
            b.position().add(0, 0, 1), Direction.NORTH, b.blockPosition().south(), false);
    var first =
        event("server.AmmoHitBlockEvent", h.getLevel(), hit, Blocks.STONE.defaultBlockState(), b);
    post(first);
    h.assertTrue(
        ((net.neoforged.bus.api.ICancellableEvent) first).isCanceled(),
        "Native kinetic impact not redirected");
    TacZCompat.tick(b);
    h.assertTrue(b.getDeltaMovement().z < 0, "Bullet failed to reflect");
    var second =
        event("server.AmmoHitBlockEvent", h.getLevel(), hit, Blocks.STONE.defaultBlockState(), b);
    post(second);
    h.assertTrue(
        !((net.neoforged.bus.api.ICancellableEvent) second).isCanceled(),
        "More than one bounce allowed");
    var explosive = bullet(h, p);
    var field = explosive.getClass().getDeclaredField("explosion");
    field.setAccessible(true);
    field.setBoolean(explosive, true);
    var rocket =
        event(
            "server.AmmoHitBlockEvent",
            h.getLevel(),
            hit,
            Blocks.STONE.defaultBlockState(),
            explosive);
    post(rocket);
    h.assertTrue(
        !((net.neoforged.bus.api.ICancellableEvent) rocket).isCanceled(),
        "Explosive native behavior replaced");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void duplicateImpactCannotDeletePendingRicochet(GameTestHelper h) throws Exception {
    var p = player(h);
    equip(p, Augment.BALLISTIC_COPROCESSOR, 5, 0);
    var b = bullet(h, p);
    var hit =
        new BlockHitResult(
            b.position().add(0, 0, .5), Direction.NORTH, b.blockPosition().south(), false);
    for (int i = 0; i < 2; i++) {
      var e =
          event("server.AmmoHitBlockEvent", h.getLevel(), hit, Blocks.STONE.defaultBlockState(), b);
      post(e);
      h.assertTrue(
          ((net.neoforged.bus.api.ICancellableEvent) e).isCanceled(),
          "Repeated impact removed pending ricochet");
    }
    h.assertTrue(TacZCompat.completeBounce(b), "Pending reflection was lost");
    h.assertTrue(
        b.getDeltaMovement().z < 0 && b.isAlive(),
        "Pending reflection did not produce a live outgoing round");
    var second =
        event("server.AmmoHitBlockEvent", h.getLevel(), hit, Blocks.STONE.defaultBlockState(), b);
    post(second);
    h.assertTrue(
        !((net.neoforged.bus.api.ICancellableEvent) second).isCanceled(),
        "A completed bounce became infinite ricochets");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "readOnlyBulletQuery", timeoutTicks = 80)
  public static void bulletGeometryQueriesCannotApplyLiveImpacts(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.BALLISTIC_COPROCESSOR, 5, 0);
    var b = bullet(h, p);
    var from = b.position();
    var velocity = b.getDeltaMovement();
    var wall = b.blockPosition().offset(0, 0, 2);
    h.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
    for (int i = 0; i < 2; i++) {
      var hit = TacZCompat.clip(h.getLevel(), from, from.add(0, 0, 4), b);
      h.assertTrue(
          hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(wall),
          "Read-only query missed the actual wall");
      h.assertTrue(
          b.isAlive() && b.position().equals(from) && b.getDeltaMovement().equals(velocity),
          "Geometry query changed a live bullet");
      h.assertTrue(
          !b.getPersistentData().getCompound(TacZCompat.DATA).getBoolean("bounced"),
          "Geometry query triggered a real ricochet");
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "tweaksTraceIsolation", timeoutTicks = 80)
  public static void tweaksIntegratedThreadsCannotOverwriteActiveTrace(GameTestHelper h)
      throws Exception {
    if (!net.neoforged.fml.ModList.get().isLoaded("tacztweaks")) {
      h.succeed();
      return;
    }
    var p = player(h);
    var bullet = bullet(h, p);
    var context =
        new net.minecraft.world.level.ClipContext(
            bullet.position(),
            bullet.position().add(0, 0, 2),
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            bullet);
    var geometry =
        new net.minecraft.world.level.ClipContext(
            p.position(),
            p.position().add(0, 0, 2),
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            p);
    var tracer = Class.forName("me.muksc.tacztweaks.feature.raytracer.BulletRayTracer");
    var args = Class.forName(tracer.getName() + "$RayTraceBlocksArgs");
    var ctor =
        args.getConstructor(
            net.minecraft.world.level.Level.class, net.minecraft.world.level.ClipContext.class);
    var function = Class.forName("kotlin.jvm.functions.Function1");
    var method = tracer.getMethod("rayTraceBlocks", args, function);
    var state = tracer.getDeclaredField("level");
    state.setAccessible(true);
    var entered = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var attempted = new java.util.concurrent.CountDownLatch(1);
    var finished = new java.util.concurrent.CountDownLatch(1);
    var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    var preserved = new java.util.concurrent.atomic.AtomicBoolean();
    var miss =
        BlockHitResult.miss(context.getTo(), Direction.SOUTH, BlockPos.containing(context.getTo()));
    Object firstCallback =
        Proxy.newProxyInstance(
            function.getClassLoader(),
            new Class[] {function},
            (proxy, m, a) -> {
              entered.countDown();
              if (!release.await(3, java.util.concurrent.TimeUnit.SECONDS))
                throw new IllegalStateException("Trace probe timed out");
              preserved.set(state.get(null) == h.getLevel());
              return miss;
            });
    Object secondCallback =
        Proxy.newProxyInstance(
            function.getClassLoader(), new Class[] {function}, (proxy, m, a) -> miss);
    var first =
        new Thread(
            () -> {
              try {
                method.invoke(null, ctor.newInstance(h.getLevel(), context), firstCallback);
              } catch (Throwable t) {
                failure.compareAndSet(null, t);
              }
            },
            "cybernetics-trace-probe-first");
    var second =
        new Thread(
            () -> {
              try {
                attempted.countDown();
                method.invoke(null, ctor.newInstance(h.getLevel(), geometry), secondCallback);
              } catch (Throwable t) {
                failure.compareAndSet(null, t);
              } finally {
                finished.countDown();
              }
            },
            "cybernetics-trace-probe-second");
    first.setDaemon(true);
    second.setDaemon(true);
    boolean overlapped;
    try {
      first.start();
      h.assertTrue(
          entered.await(2, java.util.concurrent.TimeUnit.SECONDS), "First trace did not enter");
      second.start();
      h.assertTrue(
          attempted.await(2, java.util.concurrent.TimeUnit.SECONDS),
          "Second trace did not attempt entry");
      overlapped = finished.await(250, java.util.concurrent.TimeUnit.MILLISECONDS);
    } finally {
      release.countDown();
      first.join(3000);
      second.join(3000);
    }
    h.assertTrue(
        !first.isAlive() && !second.isAlive() && failure.get() == null,
        "Trace isolation probe failed: " + failure.get());
    h.assertTrue(
        !overlapped && preserved.get(), "Concurrent trace replaced the live bullet context");
    h.assertTrue(state.get(null) == null, "Trace context leaked after completion");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "realBank", timeoutTicks = 80)
  public static void nativeBulletActuallyRicochetsAndLeavesTheWall(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.BALLISTIC_COPROCESSOR, 5, 0);
    var wall = p.blockPosition().offset(0, 1, 4);
    for (int x = -3; x <= 3; x++)
      for (int y = -2; y <= 3; y++)
        h.getLevel().setBlock(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
    var b = bullet(h, p);
    b.setPos(wall.getX() + .5, wall.getY() + .5, wall.getZ() - .4);
    b.setDeltaMovement(0, 0, 2);
    b.tick();
    h.assertTrue(
        b.isAlive()
            && b.getPersistentData().getCompound(TacZCompat.DATA).getBoolean("bounced")
            && !b.getPersistentData().getCompound(TacZCompat.DATA).getBoolean("pending")
            && b.getZ() < wall.getZ()
            && b.getDeltaMovement().z < 0,
        "Native collision did not complete ricochet outside wall");
    double first = b.getZ();
    b.tick();
    h.assertTrue(
        b.isAlive() && b.getZ() < first - .5,
        "Native round did not continue moving after ricochet");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "realBankDamage", timeoutTicks = 80)
  public static void ricochetedNativeGunRoundDamagesAnEnemy(GameTestHelper h) throws Exception {
    realRoundDamage(h, false);
  }

  @GameTest(template = "empty", batch = "slowBankDamage", timeoutTicks = 80)
  public static void fractionalNativeRoundRicochetsAndDamagesAnEnemy(GameTestHelper h)
      throws Exception {
    realRoundDamage(h, true);
  }

  private static void realRoundDamage(GameTestHelper h, boolean slow) throws Exception {
    var p = player(h);
    if (slow) {
      equip(p, Augment.DAVIDS_SANDEVISTAN, 5, 0);
      h.getLevel().addNewPlayer(p);
      Sandevistan.toggle(p);
      Sandevistan.invalidate(h.getLevel());
    }
    try {
      equip(p, Augment.BALLISTIC_COPROCESSOR, 5, 0);
      var builder =
          Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder")
              .getMethod("create")
              .invoke(null);
      call(builder, "setId", net.minecraft.resources.ResourceLocation.parse("tacz:ak47"));
      var stack = (ItemStack) call(builder, "build", p.registryAccess());
      p.getInventory().setItem(0, stack);
      p.getInventory().selected = 0;
      var index =
          (Optional<?>)
              Class.forName("com.tacz.guns.api.TimelessAPI")
                  .getMethod("getCommonGunIndex", net.minecraft.resources.ResourceLocation.class)
                  .invoke(null, net.minecraft.resources.ResourceLocation.parse("tacz:ak47"));
      var data = call(index.orElseThrow(), "getGunData");
      var bulletData = call(data, "getBulletData");
      var cache =
          Class.forName("com.tacz.guns.resource.modifier.AttachmentCacheProperty")
              .getConstructor()
              .newInstance();
      call(cache, "eval", stack, data);
      var operatorApi = Class.forName("com.tacz.guns.api.entity.IGunOperator");
      var operator = operatorApi.getMethod("fromLivingEntity", LivingEntity.class).invoke(null, p);
      call(operator, "updateCacheProperty", cache);
      var profile = TacZCompat.ballistics(p);
      h.assertTrue(
          profile != null && profile.speed() > 1 && profile.gravity() >= 0,
          "Native gun preview properties unavailable");
      var type = Class.forName("com.tacz.guns.entity.EntityKineticBullet");
      var ctor =
          Arrays.stream(type.getConstructors())
              .filter(c -> c.getParameterCount() == 8)
              .findFirst()
              .orElseThrow();
      var b =
          (Projectile)
              ctor.newInstance(
                  p.level(),
                  p,
                  stack,
                  net.minecraft.resources.ResourceLocation.parse("tacz:762x39"),
                  net.minecraft.resources.ResourceLocation.parse("tacz:ak47"),
                  true,
                  data,
                  bulletData);
      var wall = p.blockPosition().offset(0, 1, 4);
      for (int x = -2; x <= 2; x++)
        for (int y = -2; y <= 2; y++)
          h.getLevel().setBlock(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
      var target = EntityType.HUSK.create(h.getLevel());
      target.moveTo(wall.getX() + .5, wall.getY() - 1, wall.getZ() - 2, 0, 0);
      target.setNoAi(true);
      target.setNoGravity(true);
      h.getLevel().addFreshEntity(target);
      b.setPos(wall.getX() + .5, wall.getY() + .5, wall.getZ() - .4);
      b.setDeltaMovement(0, 0, 2);
      for (int i = 0; i < (slow ? 8 : 1) && b.getDeltaMovement().z > 0; i++) {
        b.tickCount++;
        Sandevistan.tickEntity(b, b::tick);
      }
      h.assertTrue(b.isAlive() && b.getDeltaMovement().z < 0, "Native shot failed to bounce");
      for (int i = 0; i < (slow ? 40 : 4) && b.isAlive(); i++) {
        b.tickCount++;
        Sandevistan.tickEntity(b, b::tick);
      }
      h.assertTrue(
          target.getHealth() < target.getMaxHealth(),
          "Ricochet produced movement without a real native damage hit");
    } finally {
      if (slow) {
        Sandevistan.stop(p, AugmentRuntime.now(p));
        h.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        Sandevistan.invalidate(h.getLevel());
      }
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void iconicSpreadUsesTheNativeLaunchMethodOnce(GameTestHelper h) throws Exception {
    var p = player(h);
    var ordinary = bullet(h, p);
    var stable = bullet(h, p);
    var launch =
        ordinary
            .getClass()
            .getMethod(
                "shootFromRotation",
                Entity.class,
                float.class,
                float.class,
                float.class,
                float.class,
                org.joml.Vector2d.class);
    launch.invoke(ordinary, p, 0f, 0f, 0f, 2f, new org.joml.Vector2d(10, 0));
    equip(p, Augment.IMMOVABLE_FORCE, 5, 0);
    launch.invoke(stable, p, 0f, 0f, 0f, 2f, new org.joml.Vector2d(10, 0));
    var a = ordinary.getDeltaMovement();
    var b = stable.getDeltaMovement();
    double first = Math.abs(a.x / a.z), second = Math.abs(b.x / b.z);
    h.assertTrue(
        first > .01 && Math.abs(second / first - .6) < .002,
        "Native spread was not reduced exactly once by 40%");
    h.assertTrue(Math.abs(a.length() - b.length()) < .0001, "Spread modifier changed bullet speed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void gunPreviewIncludesNativeGlobalSpeed(GameTestHelper h) throws Exception {
    var p = player(h);
    double multiplier =
        ((Number)
                call(
                    Class.forName("com.tacz.guns.config.common.AmmoConfig")
                        .getField("GLOBAL_BULLET_SPEED_MODIFIER")
                        .get(null),
                    "get"))
            .doubleValue();
    for (String id : List.of("ak47", "deagle")) {
      var builder =
          Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder")
              .getMethod("create")
              .invoke(null);
      call(builder, "setId", net.minecraft.resources.ResourceLocation.parse("tacz:" + id));
      var gun = (ItemStack) call(builder, "build", p.registryAccess());
      p.getInventory().setItem(0, gun);
      p.getInventory().selected = 0;
      var index =
          (Optional<?>)
              Class.forName("com.tacz.guns.api.TimelessAPI")
                  .getMethod("getCommonGunIndex", net.minecraft.resources.ResourceLocation.class)
                  .invoke(null, net.minecraft.resources.ResourceLocation.parse("tacz:" + id));
      var data = call(index.orElseThrow(), "getGunData");
      var cache =
          Class.forName("com.tacz.guns.resource.modifier.AttachmentCacheProperty")
              .getConstructor()
              .newInstance();
      call(cache, "eval", gun, data);
      var operator =
          Class.forName("com.tacz.guns.api.entity.IGunOperator")
              .getMethod("fromLivingEntity", LivingEntity.class)
              .invoke(null, p);
      call(operator, "updateCacheProperty", cache);
      var profile = TacZCompat.ballistics(p);
      double speed = ((Number) call(call(data, "getBulletData"), "getSpeed")).doubleValue();
      h.assertTrue(
          profile != null && Math.abs(profile.speed() - speed * multiplier / 20) < .0001,
          "Preview ignored the native global speed for " + id);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "fastSecondWall", timeoutTicks = 80)
  public static void fastRicochetStopsAtSecondWallEvenAfterOwnerLeaves(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.BALLISTIC_COPROCESSOR, 1, 0);
    var first = p.blockPosition().offset(0, 1, 4);
    var second = first.offset(0, 0, -10);
    for (int x = -2; x <= 2; x++)
      for (int y = -2; y <= 2; y++) {
        h.getLevel().setBlock(first.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
        h.getLevel().setBlock(second.offset(x, y, 0), Blocks.STONE.defaultBlockState(), 3);
      }
    var b = bullet(h, p);
    b.setPos(first.getX() + .5, first.getY() + .5, first.getZ() - .4);
    b.setDeltaMovement(0, 0, 25);
    b.tick();
    h.assertTrue(b.isAlive() && b.getDeltaMovement().z < 0, "Fast Tier 1 shot failed to rebound");
    b.setOwner(null);
    b.tick();
    h.assertTrue(!b.isAlive(), "Fast round passed through its second wall");
    h.assertTrue(
        b.getPersistentData().getCompound(TacZCompat.DATA).getInt("traceSteps") == 1,
        "Completed flight was lost with the owner");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "continuousFlight", timeoutTicks = 80)
  public static void davidAndCreativeBulletsMoveEveryWorldTickAndRetainLocalLifetime(
      GameTestHelper h) throws Exception {
    var p = player(h);
    h.getLevel().addNewPlayer(p);
    try {
      for (var model : List.of(Augment.DAVIDS_SANDEVISTAN, Augment.CREATIVE_SANDEVISTAN)) {
        equip(p, model, 5, 0);
        AugmentRuntime.state(p).sandyRearm = 0;
        AugmentRuntime.state(p).sandyCharge = 1;
        h.assertTrue(Sandevistan.toggle(p), "Activation failed: " + model);
        Sandevistan.invalidate(h.getLevel());
        var b = bullet(h, p);
        b.setPos(p.position().add(0, 8, 0));
        b.setDeltaMovement(0, 0, .25);
        double start = b.getZ();
        int steps = model == Augment.CREATIVE_SANDEVISTAN ? 800 : 16;
        for (int i = 0; i < steps; i++) {
          double old = b.getZ();
          b.tickCount++;
          Sandevistan.tickEntity(b, b::tick);
          h.assertTrue(b.getZ() > old, "A slow round froze on a world tick: " + model);
        }
        h.assertTrue(
            Math.abs(b.getZ() - start - .25) < .00001,
            "Fractional displacement changed native travel: " + model);
        h.assertTrue(
            Math.abs(dev.cybernetics2077.temporal.BulletTime.age(b) - 1) < .00001,
            "Bullet lifetime advanced in world time: " + model);
        h.assertTrue(b.isAlive(), "Slowed round expired before local lifetime");
        b.setPos(p.position().add(40, 8, 0));
        double old = b.getZ();
        b.tickCount++;
        Sandevistan.tickEntity(b, b::tick);
        h.assertTrue(b.getZ() - old > .2, "Leaving bubble did not restore native flight");
        b.discard();
        Sandevistan.stop(p, AugmentRuntime.now(p));
      }
    } finally {
      Sandevistan.stop(p, AugmentRuntime.now(p));
      h.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
      Sandevistan.invalidate(h.getLevel());
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void deployedBladesBlockNativeGunActions(GameTestHelper h) throws Exception {
    var p = player(h);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.ARMS.firstSlot(), new ItemStack(Cybernetics2077.MANTIS.get()));
    CyberwareRuntime.action(p, 0);
    var fire = event("common.GunFireEvent", p, ItemStack.EMPTY, LogicalSide.SERVER);
    post(fire);
    h.assertTrue(
        ((net.neoforged.bus.api.ICancellableEvent) fire).isCanceled(),
        "Gun fired with deployed blades");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void implantsAndDeckProgramsSurviveEntitySerialization(GameTestHelper h) {
    var mob = h.spawn(EntityType.HUSK, new BlockPos(2, 2, 2));
    var inv = mob.getData(Cybernetics2077.CYBERWARE);
    inv.setStackInSlot(
        CyberwareCategory.HANDS.firstSlot(),
        new ItemStack(Cybernetics2077.augment(Augment.SMART_LINK, 3)));
    var tag = new net.minecraft.nbt.CompoundTag();
    mob.saveWithoutId(tag);
    var clone = EntityType.HUSK.create(h.getLevel());
    clone.load(tag);
    h.assertTrue(
        clone.getData(Cybernetics2077.CYBERWARE).capacityUsed() == inv.capacityUsed(),
        "Installed cyberware lost across entity save/load");
    var p = player(h);
    var deck = new ItemStack(Cybernetics2077.CYBERDECK.get());
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), deck);
    var menu =
        new dev.cybernetics2077.deck.CyberdeckMenu(
            1, p.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.NULL, deck);
    menu.getSlot(0).set(new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get()));
    h.assertTrue(
        dev.cybernetics2077.deck.CyberdeckItem.programs(deck.copy()).size() == 1,
        "Quickhack lost from deck item data");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void nativeGunIntervalsFollowWorldTimeAndPreserveReloadData(GameTestHelper h)
      throws Exception {
    var p = player(h);
    var mob = h.spawn(EntityType.HUSK, new BlockPos(4, 90, 4));
    mob.setNoAi(true);
    Object builder =
        Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder")
            .getMethod("create")
            .invoke(null);
    call(
        builder,
        "setId",
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "ak47"));
    ItemStack gun = (ItemStack) call(builder, "build");
    var index =
        (Optional<?>)
            Class.forName("com.tacz.guns.api.TimelessAPI")
                .getMethod("getCommonGunIndex", net.minecraft.resources.ResourceLocation.class)
                .invoke(
                    null,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "ak47"));
    Object data = call(index.orElseThrow(), "getGunData"),
        mode = call(gun.getItem(), "getFireMode", gun);
    long nativeInterval = ((Number) call(data, "getShootInterval", mob, mode, gun)).longValue();
    Object reload = call(data, "getReloadData"),
        feed = call(reload, "getFeed"),
        cooldown = call(reload, "getCooldown");
    float emptyFeed = ((Number) call(feed, "getEmptyTime")).floatValue();
    float tacticalFeed = ((Number) call(feed, "getTacticalTime")).floatValue();
    float emptyCooldown = ((Number) call(cooldown, "getEmptyTime")).floatValue();
    float tacticalCooldown = ((Number) call(cooldown, "getTacticalTime")).floatValue();
    h.getLevel().addNewPlayer(p);
    try {
      var models =
          List.of(
              Augment.DYNALAR_SANDEVISTAN,
              Augment.MILITECH_APOGEE,
              Augment.DAVIDS_SANDEVISTAN,
              Augment.CREATIVE_SANDEVISTAN);
      double[] worldPaces = {.625, .1875, .0625, .00125};
      for (int i = 0; i < models.size(); i++) {
        equip(p, models.get(i), 5, 0);
        AugmentRuntime.state(p).sandyRearm = 0;
        AugmentRuntime.state(p).sandyCharge = 1;
        h.assertTrue(Sandevistan.toggle(p), "Failed to activate " + models.get(i));
        Sandevistan.invalidate(h.getLevel());
        long expected = Math.round(nativeInterval / worldPaces[i]);
        long nearby = ((Number) call(data, "getShootInterval", mob, mode, gun)).longValue();
        long own = ((Number) call(data, "getShootInterval", p, mode, gun)).longValue();
        h.assertTrue(
            own == expected && nearby == expected && own > nativeInterval,
            "Owner gun inherited personal time instead of world time: "
                + models.get(i)
                + " / "
                + own);
        h.assertTrue(
            Math.abs(Sandevistan.playerPace(p) - 1.25) < .00001,
            "Gun correction changed player movement/recovery");
        h.assertTrue(
            call(data, "getReloadData") == reload
                && ((Number) call(feed, "getEmptyTime")).floatValue() == emptyFeed
                && ((Number) call(feed, "getTacticalTime")).floatValue() == tacticalFeed
                && ((Number) call(cooldown, "getEmptyTime")).floatValue() == emptyCooldown
                && ((Number) call(cooldown, "getTacticalTime")).floatValue() == tacticalCooldown,
            "Sandevistan changed native reload timing");
        var reloadEvent = event("common.GunReloadEvent", p, gun, LogicalSide.SERVER);
        post(reloadEvent);
        h.assertTrue(
            !((net.neoforged.bus.api.ICancellableEvent) reloadEvent).isCanceled(),
            "Sandevistan blocked an ordinary reload");
        Sandevistan.stop(p, AugmentRuntime.now(p));
      }
      h.assertTrue(
          ((Number) call(data, "getShootInterval", p, mode, gun)).longValue() == nativeInterval,
          "Firing interval did not return to normal");
    } finally {
      Sandevistan.stop(p, AugmentRuntime.now(p));
      h.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
      Sandevistan.invalidate(h.getLevel());
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void ownGunBulletsUseWorldClockWhileArrowsKeepPersonalClock(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.DAVIDS_SANDEVISTAN, 5, 0);
    h.getLevel().addNewPlayer(p);
    try {
      Sandevistan.toggle(p);
      Sandevistan.invalidate(h.getLevel());
      var b = bullet(h, p);
      var arrow = EntityType.ARROW.create(h.getLevel());
      arrow.setOwner(p);
      arrow.setPos(p.position());
      h.assertTrue(
          Math.abs(Sandevistan.pace(b) - .0625) < .00001,
          "Owner's native TaCZ bullet bypassed slow motion");
      h.assertTrue(
          Math.abs(Sandevistan.pace(arrow) - 1.25) < .00001, "Gun change also altered arrows");
      int[] steps = {0};
      for (int i = 0; i < 160; i++) Sandevistan.tickEntity(b, () -> steps[0]++);
      h.assertTrue(
          steps[0] == 160, "Kinetic bullets did not receive a fractional step every world tick");
      b.setPos(p.position().add(40, 0, 0));
      h.assertTrue(Sandevistan.pace(b) == 1, "Gun bullet remained slowed outside the bubble");
    } finally {
      Sandevistan.stop(p, AugmentRuntime.now(p));
      h.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
      Sandevistan.invalidate(h.getLevel());
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void bulletTrajectoryPacketRetainsHighVelocitiesAndEntityIdentity(
      GameTestHelper h) {
    var buffer =
        new net.minecraft.network.RegistryFriendlyByteBuf(
            io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
    try {
      var packet = new CyberwareNetwork.Ballistics(99, UUID.randomUUID(), 20, 90, 30, 55, -4, 42);
      CyberwareNetwork.Ballistics.CODEC.encode(buffer, packet);
      var read = CyberwareNetwork.Ballistics.CODEC.decode(buffer);
      h.assertTrue(
          read.equals(packet),
          "Ballistics synchronization clamped fast bullet velocity or lost UUID");
    } finally {
      buffer.release();
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void gunMicrogeneratorOnlyTriggersAfterConfirmedDamage(GameTestHelper h)
      throws Exception {
    var p = player(h);
    equip(p, Augment.MICROGENERATOR, 5, 0);
    var target = h.spawn(EntityType.HUSK, new BlockPos(4, 90, 4));
    var b = bullet(h, p);
    var pre =
        event(
            "common.EntityHurtByGunEvent$Pre",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            false,
            1f,
            LogicalSide.SERVER);
    post(pre);
    var untouched =
        event(
            "common.EntityHurtByGunEvent$Post",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            false,
            1f,
            LogicalSide.SERVER);
    post(untouched);
    h.assertTrue(AugmentRuntime.state(p).microReady == 0, "A blocked hit triggered Microgenerator");
    post(
        event(
            "common.EntityHurtByGunEvent$Pre",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            false,
            1f,
            LogicalSide.SERVER));
    target.setHealth(target.getHealth() - 2);
    post(
        event(
            "common.EntityHurtByGunEvent$Post",
            b,
            target,
            p,
            Cybernetics2077.id("test"),
            Cybernetics2077.id("test"),
            10f,
            null,
            false,
            1f,
            LogicalSide.SERVER));
    h.assertTrue(
        AugmentRuntime.state(p).microReady > AugmentRuntime.now(p),
        "Confirmed gun damage did not start discharge cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void basicOpticsKeepSmartLockAcrossEquipmentTicks(GameTestHelper h) {
    var p = player(h);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.FACE.firstSlot(), new ItemStack(Cybernetics2077.OPTICS.get()));
    equip(p, Augment.SMART_LINK, 5, 0);
    var target = h.spawn(EntityType.HUSK, new BlockPos(2, 90, 7));
    HandsRuntime.lock(p, target);
    h.assertTrue(HandsRuntime.locked(p) == target, "Basic optics could not acquire a lock");
    for (int i = 0; i < 5; i++) AugmentRuntime.tick(p);
    h.assertTrue(
        HandsRuntime.locked(p) == target, "Equipment tick cleared a valid basic-optics lock");
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.FACE.firstSlot(), ItemStack.EMPTY);
    AugmentRuntime.tick(p);
    h.assertTrue(
        HandsRuntime.locked(p) == null && AugmentRuntime.state(p).smartTarget == null,
        "Removing optics left a live lock");
    h.succeed();
  }

  @GameTest(batch = "movingGuidance", template = "empty", timeoutTicks = 80)
  public static void enhancedGuidanceLeadsMovingNativeRoundsWithoutSpeedOrReverseSnap(
      GameTestHelper h) throws Exception {
    var p = player(h);
    equip(p, Augment.SMART_LINK, 5, 0);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.FACE.firstSlot(), new ItemStack(Cybernetics2077.OPTICS.get()));
    var target = h.spawn(EntityType.HUSK, new BlockPos(2, 90, 20));
    target.setNoAi(true);
    target.setNoGravity(true);
    target.setPos(p.position().add(0, 0, 20));
    target.xo = target.getX() - .35;
    target.yo = target.getY();
    target.zo = target.getZ();
    HandsRuntime.lock(p, target);
    var b = bullet(h, p);
    double speed = b.getDeltaMovement().length();
    TacZCompat.tick(b);
    h.assertTrue(
        b.getDeltaMovement().x > .03
            && Math.abs(b.getDeltaMovement().length() - speed) < .00001
            && b.getDeltaMovement().z > 0,
        "Predictive native guidance lost lead/speed/direction");
    target.setPos(p.position().add(0, 0, -20));
    var before = b.getDeltaMovement();
    TacZCompat.tick(b);
    h.assertTrue(
        before.distanceTo(b.getDeltaMovement()) < .00001,
        "Guidance reversed native bullet toward a target behind it");
    target.discard();
    h.succeed();
  }

  @GameTest(batch = "expandedGun", template = "empty", timeoutTicks = 60)
  public static void gunMechanismRecoveryUsesNewImplantsAndWeaponJamsBlockNativeFire(
      GameTestHelper h) throws Exception {
    var p = player(h);
    var raw = TacZCompat.interval(p, 100);
    equip(p, Augment.CLUTCH_PADDING, 5, 0);
    h.assertTrue(
        TacZCompat.interval(p, 100) < raw,
        "Clutch Padding did not affect actual gun mechanism interval");
    ExpansionRuntime.extra(p).putLong("jammedUntil", p.level().getGameTime() + 60);
    h.assertTrue(TacZCompat.blocked(p), "Quickhack jam did not reach native firing permission");
    h.succeed();
  }
}
