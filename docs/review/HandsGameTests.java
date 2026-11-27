package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mixin.LivingDamageMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class HandsGameTests {
  static void equip(ServerPlayer p, Augment a, int tier, int socket) {
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.HANDS.firstSlot() + socket,
            new ItemStack(Cybernetics2077.augment(a, tier)));
  }

  static Arrow arrow(ServerPlayer p) {
    var a = new Arrow(p.serverLevel(), p, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
    a.setDeltaMovement(0, 0, 2);
    HandsRuntime.launch(a, p);
    return a;
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void sixHandsFamiliesAndFourBladeFamiliesHaveGatedStock(GameTestHelper h) {
    var offers = dev.cybernetics2077.city.CyberwareCatalogue.offers();
    h.assertTrue(
        offers.get(74).item() == Cybernetics2077.RAVEN_TIERS.getFirst().get(),
        "Old saved stock indexes moved");
    int count = 0;
    for (var a : Augment.values()) if (a.category == CyberwareCategory.HANDS) count++;
    h.assertTrue(count == 9, "Missing hands families");
    var p = AugmentGameTests.player(h);
    var inv = p.getData(Cybernetics2077.CYBERWARE);
    for (var variant : MantisVariant.values())
      for (int t = 2; t <= 5; t++) {
        var item = Cybernetics2077.mantis(variant, t);
        h.assertTrue(item.tier().level == t && item.variant() == variant, "Wrong blade tier/type");
        inv.setStackInSlot(CyberwareCategory.ARMS.firstSlot(), new ItemStack(item));
        h.assertTrue(CyberwareRuntime.bladeVariant(p) == variant, "Runtime ignored installed type");
        h.assertTrue(
            offers.stream()
                .anyMatch(
                    o ->
                        o.item() == item && o.requiredStreetCred() == (item.tier().level - 1) * 10),
            "Missing gated blade stock");
      }
    h.assertTrue(
        Cybernetics2077.LEGACY_MANTIS.get().variant() == MantisVariant.PHYSICAL,
        "Legacy blade type changed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void smartLinkNeedsOpticsAndGuidanceCannotCrossABlock(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    p.setNoGravity(true);
    target.setNoGravity(true);
    equip(p, SMART_LINK, 5, 0);
    HandsRuntime.lock(p, target);
    h.assertTrue(HandsRuntime.locked(p) == null, "Smart lock without optics");
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.FACE.firstSlot(), new ItemStack(Cybernetics2077.OPTICS.get()));
    HandsRuntime.lock(p, target);
    h.assertTrue(HandsRuntime.locked(p) == target, "Legal scan target did not lock");
    var a = arrow(p);
    a.setPos(p.getX(), target.getEyeY(), p.getZ());
    a.tickCount = 1;
    HandsRuntime.tick(new EntityTickEvent.Pre(a));
    h.assertTrue(
        a.getPersistentData().getCompound(HandsRuntime.DATA).hasUUID("target"),
        "Clear guided flight lost lock");
    var wall = BlockPos.containing(a.position().lerp(target.getEyePosition(), .5));
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
        h.getLevel()
            .setBlockAndUpdate(
                wall.offset(dx, dy, 0),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    a.tickCount = 2;
    HandsRuntime.tick(new EntityTickEvent.Pre(a));
    h.assertTrue(
        !a.getPersistentData().getCompound(HandsRuntime.DATA).hasUUID("target"),
        "Guidance went through a wall");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void ricochetIsOnceOnlyAndStartsOutsideTheWallOnNextTick(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    CombatGameTests.target(h, p);
    equip(p, BALLISTIC_COPROCESSOR, 5, 0);
    var a = arrow(p);
    var point = p.position().add(0, 1, 1);
    var face =
        new BlockHitResult(
            point, net.minecraft.core.Direction.NORTH, BlockPos.containing(point), false);
    double base = a.getBaseDamage();
    var hit = new ProjectileImpactEvent(a, face);
    HandsRuntime.impact(hit);
    h.assertTrue(
        hit.isCanceled() && a.getZ() < point.z && a.getDeltaMovement().lengthSqr() == 0,
        "Bounce was not deferred outside the wall");
    a.tickCount = 1;
    HandsRuntime.tick(new EntityTickEvent.Pre(a));
    h.assertTrue(
        a.getDeltaMovement().z < 0 && a.getBaseDamage() > base, "Bounce direction/damage was lost");
    var twice = new ProjectileImpactEvent(a, face);
    HandsRuntime.impact(twice);
    h.assertTrue(!twice.isCanceled(), "Second ricochet allowed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void realArrowTickDoesNotTunnelThroughASurfaceAfterRicochet(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    CombatGameTests.target(h, p);
    equip(p, BALLISTIC_COPROCESSOR, 5, 0);
    var wall = p.blockPosition().above();
    for (int dx = -1; dx <= 1; dx++)
      for (int dy = -1; dy <= 1; dy++)
        h.getLevel()
            .setBlockAndUpdate(
                wall.offset(dx, dy, 0),
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
    var a = arrow(p);
    a.setPos(wall.getX() + .5, wall.getY() + .5, wall.getZ() - .4);
    a.setDeltaMovement(0, 0, 2);
    a.tick();
    h.assertTrue(
        a.getPersistentData().getCompound(HandsRuntime.DATA).getBoolean("bounced")
            && a.getZ() < wall.getZ(),
        "Native collision tunneled through wall");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void tridentContactStopsGuidanceAndNeverRicochets(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    equip(p, SMART_LINK, 5, 0);
    equip(p, BALLISTIC_COPROCESSOR, 5, 1);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            CyberwareCategory.FACE.firstSlot(), new ItemStack(Cybernetics2077.OPTICS.get()));
    HandsRuntime.lock(p, target);
    var trident = new ThrownTrident(p.serverLevel(), p, new ItemStack(Items.TRIDENT));
    trident.setDeltaMovement(0, 0, 2);
    HandsRuntime.launch(trident, p);
    h.assertTrue(
        trident.getPersistentData().getCompound(HandsRuntime.DATA).getInt("bounce") == 0,
        "Coprocessor modifies loyalty trident bounces");
    HandsRuntime.impact(new ProjectileImpactEvent(trident, new EntityHitResult(target)));
    trident.tickCount = 1;
    trident.setDeltaMovement(0, 0, -.3);
    HandsRuntime.tick(new EntityTickEvent.Pre(trident));
    h.assertTrue(trident.getDeltaMovement().z == -.3, "Return flight was retargeted");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void microgeneratorAddsRealElectricalDamageAndPreservesPrimaryHurtMemory(
      GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    equip(p, MICROGENERATOR, 5, 0);
    target.hurt(p.damageSources().playerAttack(p), 4);
    h.assertTrue(
        Math.abs(target.getHealth() - 84) < .02,
        "Microgenerator's extra damage was lost to hit immunity");
    h.assertTrue(
        target.invulnerableTime == 20
            && Math.abs(((LivingDamageMemory) target).cybernetics$lastHurt() - 4) < .01,
        "Secondary damage overwrote primary hit memory");
    long ready = AugmentRuntime.state(p).microReady;
    var saved = AugmentRuntime.state(p).serializeNBT(p.registryAccess());
    AugmentRuntime.state(p).deserializeNBT(p.registryAccess(), saved);
    h.assertTrue(
        AugmentRuntime.state(p).microReady == ready, "Generator cooldown reset by persistence");
    target.invulnerableTime = 0;
    target.hurt(p.damageSources().playerAttack(p), 4);
    h.assertTrue(Math.abs(target.getHealth() - 80) < .02, "Generator retriggered during cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void handleWrapCannotBeRefreshedByRepeatedEquipping(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    CombatGameTests.target(h, p);
    equip(p, HANDLE_WRAP, 5, 0);
    HandsRuntime.primeWrap(p);
    AugmentRuntime.tick(p); // Handle Wrap does not require optics or a scan.
    var s = AugmentRuntime.state(p);
    long end = s.wrapEnd;
    HandsRuntime.primeWrap(p);
    h.assertTrue(
        s.wrapEnd == end && Math.abs(HandsRuntime.wrapChance(p) - .2f) < .0001,
        "Wrap refresh/critical bonus wrong");
    s.wrapEnd = AugmentRuntime.now(p) - 1;
    HandsRuntime.primeWrap(p);
    h.assertTrue(HandsRuntime.wrapChance(p) == 0, "Expired wrap bypassed its rearm");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void shockAbsorberOnlyWorksDuringRangedItemUse(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    CombatGameTests.target(h, p);
    equip(p, SHOCK_ABSORBER, 5, 0);
    var idle = new LivingKnockBackEvent(p, 1, 1, 0);
    HandsRuntime.knockback(idle);
    h.assertTrue(idle.getStrength() == 1, "Idle knockback was reduced");
    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
    p.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
    var aim = new LivingKnockBackEvent(p, 1, 1, 0);
    HandsRuntime.knockback(aim);
    h.assertTrue(Math.abs(aim.getStrength() - .2) < .001, "Aiming stability was absent");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void elementalLungeStatusesAndTypedDamageWork(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    MantisEffects.hit(p, target, MantisVariant.THERMAL, 5, true);
    h.assertTrue(target.isOnFire(), "Thermal lunge did not ignite");
    target.clearFire();
    MantisEffects.hit(p, target, MantisVariant.ELECTRICAL, 5, true);
    h.assertTrue(
        target.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Shock missing");
    MantisEffects.hit(p, target, MantisVariant.TOXIC, 5, true);
    h.assertTrue(
        target.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Toxic poison missing");
    h.assertTrue(
        MantisVariant.THERMAL.source(p).is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
            && !MantisVariant.TOXIC.source(p).is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),
        "Elemental tags changed armor/fire rules");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void physicalBleedIsAttributedAndDoesNotStackOnRefresh(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    target.setNoGravity(true);
    MantisEffects.hit(p, target, MantisVariant.PHYSICAL, 5, true);
    MantisEffects.hit(p, target, MantisVariant.PHYSICAL, 5, true);
    h.runAfterDelay(
        22,
        () -> {
          h.assertTrue(
              p.isAlive(), "Bleed owner must remain strongly retained by this FakePlayer fixture");
          MantisEffects.tick(new EntityTickEvent.Post(target));
          h.assertTrue(
              target.getHealth() < 100 && target.getHealth() > 97,
              "Bleed did not tick once or stacked");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void iconicImmovableForceWorksWhileMovingAndPreservesNoGravity(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    CombatGameTests.target(h, p);
    equip(p, IMMOVABLE_FORCE, 5, 0);
    var bow = new ItemStack(Items.BOW);
    p.setOnGround(true);
    p.setDeltaMovement(Vec3.ZERO);
    h.startSequence()
        .thenWaitUntil(
            () ->
                h.assertTrue(
                    h.getLevel().getGameTime() % HandsRules.chargePeriod(5) == 0,
                    "Await charge period"))
        .thenExecute(
            () -> {
              var charge = new LivingEntityUseItemEvent.Tick(p, bow, 100);
              HandsRuntime.charging(charge);
              h.assertTrue(charge.getDuration() == 99, "Stationary charge did not accelerate");
              p.setOnGround(false);
              var air = new LivingEntityUseItemEvent.Tick(p, bow, 100);
              HandsRuntime.charging(air);
              h.assertTrue(air.getDuration() == 99, "Iconic airborne charge did not accelerate");
              p.setOnGround(true);
              p.setDeltaMovement(.4, 0, 0);
              var moving = new LivingEntityUseItemEvent.Tick(p, bow, 100);
              HandsRuntime.charging(moving);
              h.assertTrue(moving.getDuration() == 99, "Iconic moving charge did not accelerate");
              var a = arrow(p);
              a.setNoGravity(true);
              a.tickCount = 1;
              HandsRuntime.tick(new EntityTickEvent.Pre(a));
              h.assertTrue(a.getDeltaMovement().y == 0, "No-gravity arrow was pushed upward");
            })
        .thenSucceed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void deferredRicochetCompletesEvenIfItsOwnerIsGone(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    CombatGameTests.target(h, p);
    equip(p, BALLISTIC_COPROCESSOR, 5, 0);
    var a = arrow(p);
    var point = p.position().add(0, 1, 1);
    HandsRuntime.impact(
        new ProjectileImpactEvent(
            a,
            new BlockHitResult(
                point, net.minecraft.core.Direction.NORTH, BlockPos.containing(point), false)));
    a.setOwner(null);
    HandsRuntime.tick(new EntityTickEvent.Pre(a));
    h.assertTrue(a.getDeltaMovement().z < 0, "Orphaned arrow lost its pending rebound");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void killingHitDischargesOntoAtMostFourHostilesNotBystanders(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var main = CombatGameTests.target(h, p);
    equip(p, MICROGENERATOR, 5, 0);
    var zombies = new java.util.ArrayList<net.minecraft.world.entity.monster.Zombie>();
    for (int i = 0; i < 5; i++) {
      var z = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 20, 5));
      z.setNoAi(true);
      z.setNoGravity(true);
      z.setPos(main.position().add(.5 + i * .12, 0, 0));
      zombies.add(z);
    }
    var cow = h.spawn(EntityType.COW, new BlockPos(3, 20, 5));
    cow.setNoAi(true);
    cow.setPos(main.position().add(.4, 0, 0));
    float passiveHealth = cow.getHealth();
    main.hurt(p.damageSources().playerAttack(p), 1000);
    h.assertTrue(!main.isAlive(), "Fixture kill did not kill");
    h.assertTrue(
        zombies.stream().filter(z -> z.getHealth() < z.getMaxHealth()).count() == 4,
        "Killing-hit discharge was absent or exceeded four targets");
    h.assertTrue(cow.getHealth() == passiveHealth, "Burst hit an uninvolved passive animal");
    h.succeed();
  }
}
