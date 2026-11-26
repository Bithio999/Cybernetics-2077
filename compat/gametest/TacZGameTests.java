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
  public static void nativeGunIntervalUsesPersonalTimeWithoutChangingSharedGunData(GameTestHelper h)
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
    equip(p, Augment.MILITECH_APOGEE, 5, 0);
    h.getLevel().addNewPlayer(p);
    try {
      Sandevistan.toggle(p);
      Sandevistan.invalidate(h.getLevel());
      long slowed = ((Number) call(data, "getShootInterval", mob, mode, gun)).longValue();
      h.assertTrue(
          slowed > nativeInterval, "Native TaCZ interval ignored nearby Sandevistan field");
      long own = ((Number) call(data, "getShootInterval", p, mode, gun)).longValue();
      h.assertTrue(
          own == TacZCompat.interval(p, nativeInterval),
          "Native gun interval did not use the owner's relative pace");
    } finally {
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
}
