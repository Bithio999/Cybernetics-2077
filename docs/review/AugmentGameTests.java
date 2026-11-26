package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.combat.CyberwareRuntime;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class AugmentGameTests {
  static void equip(ServerPlayer p, Augment kind, int tier, int offset) {
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(
            kind.category.firstSlot() + offset, new ItemStack(Cybernetics2077.augment(kind, tier)));
  }

  static ServerPlayer player(GameTestHelper h) {
    var p =
        new net.neoforged.neoforge.common.util.FakePlayer(
            h.getLevel(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "AugmentTest")) {
          @Override
          public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
            return false;
          }

          @Override
          public void die(net.minecraft.world.damagesource.DamageSource source) {
            // FakePlayer normally suppresses death hooks; restore the native cancellation hook.
            net.neoforged.neoforge.common.CommonHooks.onLivingDeath(this, source);
          }

          @Override
          public boolean canHarmPlayer(net.minecraft.world.entity.player.Player target) {
            return true;
          }
        };
    p.setGameMode(GameType.SURVIVAL);
    p.getAbilities().invulnerable = false;
    try {
      var protection = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
      protection.setAccessible(true);
      protection.setInt(p, 0);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException(ex);
    }
    p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
    p.setHealth(100);
    p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 90, 2))));
    p.setNoGravity(true);
    return p;
  }

  static void ground(ServerPlayer p) {
    p.serverLevel().setBlock(p.blockPosition().below(), Blocks.STONE.defaultBlockState(), 3);
    p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    p.setOnGround(true);
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void researchedStartingTiersAndHandsCoverage(GameTestHelper h) {
    int count = 0;
    for (var kind : Augment.values()) {
      for (int t = kind.firstTier; t <= 5; t++) {
        var item = Cybernetics2077.augment(kind, t);
        h.assertTrue(
            item.tier().level == t && item.category() == kind.category, "Incorrect tier/category");
        count++;
      }
      if (kind.firstTier > 1)
        h.assertTrue(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    Cybernetics2077.id(kind.id + "_tier_" + (kind.firstTier - 1)))
                == Items.AIR,
            "Unavailable lower tier registered");
    }
    h.assertTrue(Augment.values().length == 37 && count == 144, "Wrong expansion size");
    h.assertTrue(
        Cybernetics2077.LEGACY_MANTIS.get().tier() == CyberwareTier.TWO
            && Cybernetics2077.MANTIS_TIERS.size() == 4,
        "Legacy Mantis did not become tier two");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void everyNewVariantInstallsAndDuplicatesAreRejected(GameTestHelper h) {
    var p = player(h);
    var inv = p.getData(Cybernetics2077.CYBERWARE);
    for (var kind : Augment.values())
      for (int t = kind.firstTier; t <= 5; t++) {
        for (int i = 0; i < inv.getSlots(); i++) inv.setStackInSlot(i, ItemStack.EMPTY);
        var menu = new CyberneticsBenchMenu(1, p.getInventory());
        p.getInventory().setItem(0, new ItemStack(Cybernetics2077.augment(kind, t)));
        menu.quickMoveStack(p, 48);
        h.assertTrue(
            AugmentRuntime.tier(p, kind) == t && menu.capacityUsed() == kind.capacity,
            "Failed native installation: " + kind + " " + t);
        if (kind.category.sockets > 1)
          h.assertTrue(
              !menu.getSlot(kind.category.firstSlot() + 1)
                  .mayPlace(new ItemStack(Cybernetics2077.augment(kind, kind.firstTier))),
              "Cross-tier duplicate accepted");
        menu.quickMoveStack(p, kind.category.firstSlot());
        h.assertTrue(AugmentRuntime.tier(p, kind) == 0, "Removed implant stayed active");
        p.getInventory().clearContent();
      }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void armorAndHealthModifiersAreTransientAndReversible(GameTestHelper h) {
    var p = player(h);
    double baseArmor = p.getAttributeValue(Attributes.ARMOR);
    equip(p, BIONIC_JOINTS, 5, 0);
    equip(p, EPIMORPHIC_SKELETON, 5, 1);
    equip(p, SUBDERMAL_ARMOR, 5, 0);
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getAttributeValue(Attributes.ARMOR) == baseArmor + 19
            && Math.abs(p.getMaxHealth() - 115) < .01,
        "Armor/HP effects missing");
    p.setHealth(115);
    for (int slot : new int[] {6, 7, 17})
      p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(slot, ItemStack.EMPTY);
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getMaxHealth() == 100
            && p.getHealth() == 100
            && p.getAttributeValue(Attributes.ARMOR) == baseArmor,
        "Unequipping left attribute bonuses");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void cortexEffectsRaiseRealRamAndShortenUploads(GameTestHelper h) {
    var p = player(h);
    CombatGameTests.install(p);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(3, new ItemStack(Cybernetics2077.CYBERDECK_TIERS.get(4).get()));
    equip(p, EX_DISK, 5, 0);
    equip(p, MEMORY_BOOST, 5, 1);
    h.assertTrue(
        AugmentRuntime.maxRam(p) == 18 && AugmentRuntime.uploadTicks(p) == 10,
        "Cortex effects did not reach server RAM/upload limits");
    CyberwareRuntime.state(p).ram = 99;
    CyberwareRuntime.tick(p);
    h.assertTrue(
        CyberwareRuntime.state(p).ram == 18 && CyberwareRuntime.snapshot(p).getInt("ramMax") == 18,
        "HUD/runtime RAM mismatch");
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(0, ItemStack.EMPTY);
    CyberwareRuntime.tick(p);
    h.assertTrue(CyberwareRuntime.state(p).ram == 13, "Removing Ex-Disk left over-cap RAM");
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(3, new ItemStack(Cybernetics2077.augment(BIODYNE_BERSERK, 5)));
    h.assertTrue(AugmentRuntime.maxRam(p) == 0, "Cortex RAM enabled hacking without a deck");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void killsRestoreHpAndRam(GameTestHelper h) {
    var p = player(h);
    CombatGameTests.install(p);
    equip(p, MEMORY_BOOST, 5, 0);
    equip(p, HEAL_ON_KILL, 5, 0);
    p.setHealth(30);
    CyberwareRuntime.state(p).ram = 0;
    var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 90, 5));
    zombie.setNoAi(true);
    zombie.hurt(p.damageSources().playerAttack(p), 10000);
    h.assertTrue(
        Math.abs(p.getHealth() - 36) < .01 && Math.abs(CyberwareRuntime.state(p).ram - 1.35f) < .01,
        "Actual kill did not trigger healing/RAM");
    p.setHealth(30);
    CyberwareRuntime.state(p).ram = 0;
    var cow = h.spawn(EntityType.COW, new BlockPos(4, 90, 5));
    cow.setNoAi(true);
    p.setLastHurtMob(cow);
    cow.hurt(p.damageSources().playerAttack(p), 10000);
    h.assertTrue(
        p.getHealth() == 30 && CyberwareRuntime.state(p).ram == 0,
        "Passive animal farming triggered enemy-kill rewards");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void berserkSurvivesFatalHitsThenHealsAndHonorsCooldown(GameTestHelper h) {
    var p = player(h);
    equip(p, BIODYNE_BERSERK, 5, 0);
    h.assertTrue(AugmentRuntime.action(p, 5), "Berserk did not activate");
    p.hurt(p.damageSources().generic(), 1000);
    h.assertTrue(
        p.isAlive() && Math.abs(p.getHealth() - 25) < .01,
        "Berserk failed 25% health floor: " + p.getHealth());
    h.assertTrue(
        p.getAttributeValue(Attributes.ATTACK_SPEED)
            > p.getAttribute(Attributes.ATTACK_SPEED).getBaseValue(),
        "Berserk attack speed missing");
    h.assertTrue(AugmentRuntime.interval(p, 10) == 8, "Mantis combo ignored Berserk attack speed");
    var crit = new net.neoforged.neoforge.event.entity.player.CriticalHitEvent(p, p, 1.5f, true);
    AugmentRuntime.critical(crit);
    h.assertTrue(crit.getDamageMultiplier() == 3, "Berserk crit damage missing");
    var s = AugmentRuntime.state(p);
    s.berserkKills = 2;
    s.berserkEnd = AugmentRuntime.now(p) - 1;
    AugmentRuntime.tick(p);
    h.assertTrue(
        Math.abs(p.getHealth() - 75) < .01 && !AugmentRuntime.berserk(p),
        "Berserk expiry heal incorrect");
    h.assertTrue(!AugmentRuntime.action(p, 5), "Berserk recast bypassed cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void berserkRejectsItemUseAndRangedDamage(GameTestHelper h) {
    var p = player(h);
    equip(p, BIODYNE_BERSERK, 5, 0);
    AugmentRuntime.action(p, 5);
    var use =
        new net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start(
            p, new ItemStack(Items.GOLDEN_APPLE), 32);
    AugmentRuntime.use(use);
    h.assertTrue(use.isCanceled(), "Berserk allowed item use");
    var cow = CombatGameTests.target(h, p);
    var arrow = EntityType.ARROW.create(p.serverLevel());
    arrow.setOwner(p);
    cow.hurt(p.damageSources().arrow(arrow, p), 10);
    h.assertTrue(cow.getHealth() == 100, "Berserk allowed ranged damage");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void bloodPumpChargesRegenerationAndOfflineRecovery(GameTestHelper h) {
    var p = player(h);
    equip(p, BLOOD_PUMP, 5, 0);
    p.setHealth(10);
    h.assertTrue(
        AugmentRuntime.action(p, 6) && Math.abs(p.getHealth() - 45) < .01,
        "Pump burst hp=" + p.getHealth() + ", bio=" + AugmentRuntime.tier(p, BIOMONITOR));
    p.setHealth(10);
    h.assertTrue(
        AugmentRuntime.action(p, 6) && !AugmentRuntime.action(p, 6), "Pump charge limit failed");
    var s = AugmentRuntime.state(p);
    var restored = new AugmentState();
    restored.deserializeNBT(p.registryAccess(), s.serializeNBT(p.registryAccess()));
    h.assertTrue(
        restored.pumpCharges == 0 && restored.pumpNext == s.pumpNext,
        "Pump state reset on persistence");
    s.pumpNext = AugmentRuntime.now(p) - 1001;
    AugmentRuntime.tick(p);
    h.assertTrue(
        s.pumpCharges == 2 && s.pumpNext == 0,
        "Elapsed offline recharge did not restore both charges");
    p.setHealth(10);
    s.pumpEnd = AugmentRuntime.now(p) + 120;
    for (int i = 1; i <= 20; i++) h.runAfterDelay(i, () -> AugmentRuntime.tick(p));
    h.runAfterDelay(
        21,
        () -> {
          h.assertTrue(Math.abs(p.getHealth() - 12.5) < .01, "Pump sustained healing incorrect");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void camouflageConcealsWithoutTouchingPotionEffects(GameTestHelper h) {
    var p = player(h);
    equip(p, OPTICAL_CAMO, 5, 0);
    h.assertTrue(AugmentRuntime.action(p, 7) && p.isInvisible(), "Camo invisibility missing");
    var mob = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 90, 7));
    mob.setNoAi(true);
    mob.setTarget(p);
    h.assertTrue(mob.getTarget() == null, "Distant mob acquired cloaked player");
    mob.setPos(p.position().add(0, 0, 1));
    mob.setTarget(p);
    h.assertTrue(mob.getTarget() == p, "Camo incorrectly made contact enemies unable to attack");
    p.addEffect(
        new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.INVISIBILITY, 600));
    AugmentRuntime.state(p).camoEnd = AugmentRuntime.now(p) - 1;
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)
            && !AugmentRuntime.camouflaged(p)
            && !AugmentRuntime.state(p).wasCamo,
        "Camo removed a potion or did not expire");
    h.assertTrue(!AugmentRuntime.action(p, 7), "Camo ignored cooldown");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void clairvoyantMarksVisibleEnemiesAndRetainsThem(GameTestHelper h) {
    var p = player(h);
    equip(p, CLAIRVOYANT, 5, 0);
    var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 90, 8));
    zombie.setNoAi(true);
    zombie.setNoGravity(true);
    var cow = h.spawn(EntityType.COW, new BlockPos(5, 90, 8));
    cow.setNoAi(true);
    cow.setNoGravity(true);
    h.assertTrue(
        ScannerNetwork.installed(p) && ScannerNetwork.range(p) == 64,
        "Clairvoyant did not enable scanner");
    AugmentRuntime.state(p).scanUntil = AugmentRuntime.now(p) + 20;
    for (int i = 1; i <= 4; i++) h.runAfterDelay(i, () -> AugmentRuntime.tick(p));
    h.runAfterDelay(
        5,
        () -> {
          var s = AugmentRuntime.state(p);
          h.assertTrue(
              s.highlights.containsKey(zombie.getId()) && !s.highlights.containsKey(cow.getId()),
              "Clairvoyant enemy selection incorrect");
          s.scanUntil = 0;
          AugmentRuntime.tick(p);
          h.assertTrue(
              s.highlights.get(zombie.getId()) > AugmentRuntime.now(p) + 1100,
              "Highlights were lost on scan release");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void doubleJumpAllowsOneBoostPerRealLanding(GameTestHelper h) {
    var p = player(h);
    equip(p, REINFORCED_TENDONS, 5, 0);
    p.setOnGround(false);
    h.assertTrue(
        AugmentRuntime.action(p, 8) && Math.abs(p.getDeltaMovement().y - .595) < .001,
        "Double jump missing");
    h.assertTrue(!AugmentRuntime.action(p, 8), "Repeated air boosts accepted");
    p.setOnGround(true);
    AugmentRuntime.tick(p);
    h.assertTrue(
        AugmentRuntime.state(p).airJumped, "Forged grounded flag reset air jump without terrain");
    ground(p);
    AugmentRuntime.tick(p);
    p.setOnGround(false);
    h.assertTrue(AugmentRuntime.action(p, 8), "Landing did not restore double jump");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void fortifiedAnklesChargeReleaseAndCannotRecastInAir(GameTestHelper h) {
    var p = player(h);
    equip(p, FORTIFIED_ANKLES, 5, 0);
    ground(p);
    h.assertTrue(AugmentRuntime.action(p, 9), "Ankle charge refused on ground");
    AugmentRuntime.state(p).ankleStart = AugmentRuntime.now(p) - 20;
    h.assertTrue(
        AugmentRuntime.action(p, 12) && Math.abs(p.getDeltaMovement().y - 1) < .001,
        "Charged ankle jump incorrect");
    h.assertTrue(!AugmentRuntime.action(p, 12), "Ankle release repeated in air");
    ground(p);
    AugmentRuntime.action(p, 9);
    AugmentRuntime.action(p, 12);
    h.assertTrue(
        Math.abs(p.getDeltaMovement().y - .42) < .001,
        "Quick ankle tap did not make a normal jump");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void adrenalineRearmsAndNeofiberActuallyMitigates(GameTestHelper h) {
    var p = player(h);
    equip(p, ADRENALINE_CONVERTER, 5, 0);
    equip(p, NEOFIBER, 5, 1);
    p.getRandom().setSeed(2077);
    var attacker = EntityType.ZOMBIE.create(p.serverLevel());
    int mitigated = 0;
    for (int i = 0; i < 120; i++) {
      p.setHealth(100);
      p.invulnerableTime = 0;
      p.hurt(p.damageSources().mobAttack(attacker), 10);
      if (p.getHealth() > 90.01f) mitigated++;
    }
    h.assertTrue(
        mitigated > 0 && mitigated < 40,
        "NeoFiber never mitigated or mitigated every hit: " + mitigated);
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getAttributeValue(Attributes.MOVEMENT_SPEED)
            > p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),
        "Adrenaline movement bonus missing");
    var s = AugmentRuntime.state(p);
    s.adrenalineEnd = AugmentRuntime.now(p) - 1;
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getAttributeValue(Attributes.MOVEMENT_SPEED)
            == p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),
        "Expired adrenaline speed persisted");
    s.adrenalineReady = AugmentRuntime.now(p) - 1;
    p.invulnerableTime = 0;
    p.hurt(p.damageSources().mobAttack(attacker), 1);
    AugmentRuntime.tick(p);
    h.assertTrue(
        s.adrenalineEnd < AugmentRuntime.now(p), "An ongoing fight retriggered entry bonus");
    s.lastCombat = AugmentRuntime.now(p) - 101;
    p.invulnerableTime = 0;
    p.hurt(p.damageSources().mobAttack(attacker), 1);
    AugmentRuntime.tick(p);
    h.assertTrue(
        s.adrenalineEnd > AugmentRuntime.now(p),
        "A new fight after quiet time did not retrigger bonus");
    h.succeed();
  }
}
