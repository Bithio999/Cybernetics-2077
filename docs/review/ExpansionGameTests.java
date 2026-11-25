package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.AugmentGameTests.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class ExpansionGameTests {
  @GameTest(template = "empty", timeoutTicks = 60)
  public static void fourteenFamiliesAppendWithoutChangingSavedStockIds(GameTestHelper h) {
    var offers = CyberwareCatalogue.offers();
    h.assertTrue(offers.size() == 197, "Missing new tier variants");
    h.assertTrue(
        offers.get(74).item() == Cybernetics2077.RAVEN_TIERS.getFirst().get(),
        "Saved Raven offer index changed");
    for (var category : CyberwareCategory.values()) {
      long count =
          java.util.Arrays.stream(Augment.values())
              .filter(
                  a ->
                      a.ordinal() > FORTIFIED_ANKLES.ordinal()
                          && a.ordinal() < DYNALAR_SANDEVISTAN.ordinal()
                          && a.category == category)
              .count();
      boolean excluded =
          category == CyberwareCategory.ARMS
              || category == CyberwareCategory.HANDS
              || category == CyberwareCategory.OPERATING_SYSTEM;
      h.assertTrue(count == (excluded ? 0 : 2), "Wrong new family count in " + category);
    }
    for (var a : Augment.values())
      if (a.ordinal() > FORTIFIED_ANKLES.ordinal())
        for (int t = a.firstTier; t <= 5; t++) {
          var item = Cybernetics2077.augment(a, t);
          h.assertTrue(
              offers.stream()
                  .anyMatch(
                      o ->
                          o.item() == item
                              && o.requiredStreetCred() == (item.tier().level - 1) * 10),
              "New item missing gated shop offer");
        }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void armorToughnessStackAndUninstallCleanly(GameTestHelper h) {
    var p = player(h);
    p.getAttribute(Attributes.ARMOR).setBaseValue(10);
    equip(p, BIONIC_JOINTS, 5, 0);
    equip(p, PARA_BELLUM, 5, 1);
    equip(p, SUBDERMAL_ARMOR, 5, 0);
    AugmentRuntime.tick(p);
    h.assertTrue(
        Math.abs(p.getAttributeValue(Attributes.ARMOR) - 25.2) < .001
            && Math.abs(p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) - 4) < .001,
        "Armor multiplier/toughness wrong");
    double tough = p.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == tough, "Per-tick modifiers accumulate");
    for (int i = 0; i < 21; i++)
      p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(i, ItemStack.EMPTY);
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getAttributeValue(Attributes.ARMOR) == 10
            && p.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 0,
        "Uninstallation leaked stats");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void ramUpgradeAddsCapacityAndRecoveryOnlyWithDeck(GameTestHelper h) {
    var p = player(h);
    QuickhackGameTests.install(p, QuickhackGameTests.loaded(false, 5, QuickhackKind.SHORT_CIRCUIT));
    equip(p, RAM_UPGRADE, 5, 0);
    equip(p, SELF_ICE, 5, 1);
    var deck = (CyberdeckItem) CyberwareRuntime.deck(p).getItem();
    CyberwareRuntime.state(p).ram = 0;
    CyberwareRuntime.tick(p);
    h.assertTrue(
        AugmentRuntime.maxRam(p) == deck.ram() + 4
            && Math.abs(CyberwareRuntime.state(p).ram - (deck.ramPerSecond() + .3f) / 20) < .0001,
        "Real RAM recovery/capacity unchanged");
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, ItemStack.EMPTY);
    h.assertTrue(AugmentRuntime.maxRam(p) == 0, "RAM implants create a free cyberdeck");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void iceBlocksDamageAndControlButRespectsItsSavedRecharge(GameTestHelper h) {
    var attacker = player(h);
    var target = player(h);
    boolean previous = attacker.server.isPvpAllowed();
    attacker.server.setPvpAllowed(true);
    try {
      var deck = QuickhackGameTests.loaded(false, 5, QuickhackKind.SHORT_CIRCUIT);
      equip(target, SELF_ICE, 5, 0);
      var shortCircuit = Cybernetics2077.quickhack(QuickhackKind.SHORT_CIRCUIT, 5);
      QuickhackEffects.land(attacker, target, shortCircuit, 20, deck);
      h.assertTrue(
          target.getHealth() == 100
              && AugmentRuntime.state(target).iceReady > AugmentRuntime.now(target),
          "ICE failed: hp="
              + target.getHealth()
              + ", ready="
              + AugmentRuntime.state(target).iceReady
              + ", tier="
              + AugmentRuntime.tier(target, SELF_ICE)
              + ", allowed="
              + CyberwareRuntime.canAttack(attacker, target));
      QuickhackEffects.land(attacker, target, shortCircuit, 20, deck);
      h.assertTrue(target.getHealth() < 100, "ICE also blocked hits on cooldown");
      AugmentRuntime.state(target).iceReady = 0;
      var control = Cybernetics2077.quickhack(QuickhackKind.CRIPPLE_MOVEMENT, 5);
      QuickhackEffects.land(attacker, target, control, 0, deck);
      h.assertTrue(
          !QuickhackEffects.affected(target, QuickhackKind.CRIPPLE_MOVEMENT),
          "ICE failed to reject control program");
      var restored = new AugmentState();
      restored.deserializeNBT(
          target.registryAccess(),
          AugmentRuntime.state(target).serializeNBT(target.registryAccess()));
      h.assertTrue(
          restored.iceReady == AugmentRuntime.state(target).iceReady, "ICE recharge reset on load");
      equip(target, SELF_ICE, 3, 0);
      h.assertTrue(!AugmentExpansion.blockHack(attacker, target), "Reinstall resets ICE recharge");
    } finally {
      attacker.server.setPvpAllowed(previous);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void pvpRulesAndPainEditorMitigateQuickhacks(GameTestHelper h) {
    var p = player(h);
    var target = player(h);
    boolean previous = p.server.isPvpAllowed();
    p.server.setPvpAllowed(false);
    try {
      h.assertTrue(!CyberwareRuntime.canAttack(p, target), "PvP-off allowed hacking");
    } finally {
      p.server.setPvpAllowed(previous);
    }
    p.server.setPvpAllowed(true);
    try {
      equip(target, PAIN_EDITOR, 5, 0);
      AugmentRuntime.tick(target);
      QuickhackEffects.land(
          p,
          target,
          Cybernetics2077.quickhack(QuickhackKind.SHORT_CIRCUIT, 5),
          20,
          ItemStack.EMPTY);
      h.assertTrue(
          Math.abs(target.getHealth() - 82) < .001,
          "Pain Editor hp="
              + target.getHealth()
              + ", allowed="
              + CyberwareRuntime.canAttack(p, target));
    } finally {
      p.server.setPvpAllowed(previous);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void biomonitorSpendsRealPumpAndSafePotionThenDebounces(GameTestHelper h) {
    var p = player(h);
    equip(p, BIOMONITOR, 5, 0);
    equip(p, BLOOD_PUMP, 5, 1);
    p.setHealth(10);
    var s = AugmentRuntime.state(p);
    // Health checks occur every half second; call at the next real server interval.
    h.runAtTickTime(
        10,
        () -> {
          // Cover all ten clock phases on future ticks. A zero-delay callback scheduled while
          // executing this callback can miss the current tick's task iteration entirely.
          for (int i = 1; i <= 10; i++) h.runAfterDelay(i, () -> AugmentRuntime.tick(p));
        });
    h.runAtTickTime(
        22,
        () -> {
          h.assertTrue(
              s.pumpCharges == 1 && p.getHealth() >= 52,
              "Biomonitor did not spend one amplified pump charge: charges="
                  + s.pumpCharges
                  + ", hp="
                  + p.getHealth());
          p.getData(Cybernetics2077.CYBERWARE)
              .setStackInSlot(BLOOD_PUMP.category.firstSlot() + 1, ItemStack.EMPTY);
          p.setHealth(10);
          s.biomonitorReady = 0;
          var bad = new ItemStack(Items.POTION);
          bad.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
          var good = new ItemStack(Items.POTION);
          good.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.HEALING));
          p.getInventory().setItem(0, bad);
          p.getInventory().setItem(1, good);
          for (int i = 1; i <= 10; i++) h.runAfterDelay(i, () -> AugmentRuntime.tick(p));
        });
    h.runAtTickTime(
        34,
        () -> {
          h.assertTrue(
              p.getHealth() > 10
                  && p.getInventory().getItem(0).is(Items.POTION)
                  && p.getInventory().getItem(1).is(Items.GLASS_BOTTLE),
              "Automatic potion consumption wrong");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void secondHeartStopsFatalDamageOnceAndNeverVoid(GameTestHelper h) {
    var p = player(h);
    equip(p, SECOND_HEART, 5, 0);
    p.setHealth(5);
    p.hurt(p.damageSources().generic(), 10000);
    h.assertTrue(
        p.isAlive() && p.getHealth() == 100,
        "Heart failed: hp="
            + p.getHealth()
            + ", alive="
            + p.isAlive()
            + ", ready="
            + AugmentRuntime.state(p).heartReady);
    var s = AugmentRuntime.state(p);
    var restored = new AugmentState();
    restored.deserializeNBT(p.registryAccess(), s.serializeNBT(p.registryAccess()));
    h.assertTrue(
        restored.heartReady == s.heartReady && s.heartReady > AugmentRuntime.now(p),
        "Heart cooldown lost");
    p.invulnerableTime = 0;
    p.hurt(p.damageSources().generic(), 10000);
    h.assertTrue(!p.isAlive(), "Heart bypassed cooldown");
    var q = player(h);
    equip(q, SECOND_HEART, 5, 0);
    q.hurt(q.damageSources().fellOutOfWorld(), 10000);
    h.assertTrue(!q.isAlive(), "Heart blocks void");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void denseMarrowBuffsMeleeOnceAndCritsAreWeaponOnly(GameTestHelper h) {
    var p = player(h);
    var cow = QuickhackGameTests.target(h, p);
    equip(p, DENSE_MARROW, 5, 0);
    cow.hurt(p.damageSources().playerAttack(p), 10);
    h.assertTrue(Math.abs(cow.getHealth() - 987) < .001, "Dense Marrow missing or doubled");
    cow.invulnerableTime = 0;
    QuickhackEffects.land(
        p, cow, Cybernetics2077.quickhack(QuickhackKind.SHORT_CIRCUIT, 5), 10, ItemStack.EMPTY);
    h.assertTrue(Math.abs(cow.getHealth() - 977) < .001, "Melee bonus applied to a quickhack");
    equip(p, STABBER, 5, 0);
    equip(p, VISUAL_CORTEX_SUPPORT, 5, 1);
    cow.setPos(p.position().add(0, 0, 32));
    h.assertTrue(
        Math.abs(AugmentExpansion.weaponCritChance(p, cow, true) - .44) < .001
            && Math.abs(AugmentExpansion.weaponCritChance(p, cow, false) - .30) < .001,
        "Critical probabilities stack incorrectly");
    p.getRandom().setSeed(2077);
    int crits = 0;
    for (int i = 0; i < 100; i++) if (AugmentRuntime.bladeDamage(p, cow, 10) > 10) crits++;
    h.assertTrue(crits > 20 && crits < 70, "Mantis critical damage never rolls");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void lynxAndJenkinsApplyDifferentMovementTradeoffs(GameTestHelper h) {
    var p = player(h);
    equip(p, LYNX_PAWS, 5, 0);
    AugmentRuntime.tick(p);
    h.assertTrue(
        Math.abs(AugmentExpansion.stepVolume(p, 1) - .5) < .001
            && p.getAttributeValue(Attributes.SNEAKING_SPEED)
                > p.getAttribute(Attributes.SNEAKING_SPEED).getBaseValue(),
        "Lynx movement/steps missing");
    p.hurt(p.damageSources().fall(), 10);
    h.assertTrue(Math.abs(p.getHealth() - 92) < .001, "Lynx fall reduction missing");
    equip(p, JENKINS_TENDONS, 5, 0);
    p.setSprinting(true);
    var s = AugmentRuntime.state(p);
    s.jenkinsFatigue = 0;
    h.assertTrue(
        Math.abs(AugmentExpansion.sprintBonus(p) - .6) < .001, "Initial sprint boost wrong");
    s.jenkinsFatigue = 100;
    h.assertTrue(
        Math.abs(AugmentExpansion.sprintBonus(p) - .1) < .001, "Sprint boost never tapers");
    p.setSprinting(false);
    AugmentRuntime.tick(p);
    h.assertTrue(
        p.getAttributeValue(Attributes.MOVEMENT_SPEED)
            == p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue(),
        "Jenkins speed persists when stopped");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void stalkerSeesEnemiesThroughCoverAndDoomsayerOnlyDangers(GameTestHelper h) {
    var p = player(h);
    equip(p, STALKER, 5, 0);
    p.setYRot(0);
    p.setXRot(0);
    var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 90, 8));
    zombie.setNoAi(true);
    zombie.setNoGravity(true);
    var cow = h.spawn(EntityType.COW, new BlockPos(4, 90, 8));
    cow.setNoAi(true);
    cow.setNoGravity(true);
    p.serverLevel()
        .setBlock(p.blockPosition().offset(0, 1, 3), Blocks.STONE.defaultBlockState(), 3);
    var s = AugmentRuntime.state(p);
    s.scanUntil = AugmentRuntime.now(p) + 50;
    h.assertTrue(
        ScannerNetwork.installed(p) && ScannerNetwork.range(p) == 44,
        "Specialized optics do not scan");
    for (int i = 1; i <= 4; i++) h.runAfterDelay(i, () -> AugmentRuntime.tick(p));
    h.runAfterDelay(
        5,
        () -> {
          h.assertTrue(
              s.highlights.containsKey(zombie.getId()) && !s.highlights.containsKey(cow.getId()),
              "Stalker cover/cone selection wrong");
          equip(p, DOOMSAYER, 5, 0);
          var creeper = h.spawn(EntityType.CREEPER, new BlockPos(2, 90, 10));
          creeper.setNoAi(true);
          creeper.setNoGravity(true);
          for (int i = 1; i <= 4; i++) h.runAfterDelay(i, () -> AugmentRuntime.tick(p));
          h.runAfterDelay(
              5,
              () -> {
                h.assertTrue(
                    s.highlights.containsKey(creeper.getId())
                        && !s.highlights.containsKey(zombie.getId()),
                    "Doomsayer inherited ordinary enemy highlights");
                h.succeed();
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void shockRetaliationIsBoundedAndHasAnInternalCooldown(GameTestHelper h) {
    var p = player(h);
    equip(p, SHOCK_N_AWE, 5, 0);
    p.getRandom().setSeed(2077);
    var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 90, 4));
    zombie.setNoAi(true);
    zombie.setNoGravity(true);
    zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
    zombie.setHealth(1000);
    for (int i = 0; i < 100 && AugmentRuntime.state(p).shockReady == 0; i++) {
      p.setHealth(100);
      p.invulnerableTime = 0;
      p.hurt(p.damageSources().mobAttack(zombie), 1);
    }
    h.assertTrue(
        zombie.getHealth() == 988 && AugmentRuntime.state(p).shockReady > AugmentRuntime.now(p),
        "Shock did not hit attacker exactly once");
    for (int i = 0; i < 10; i++) {
      p.invulnerableTime = 0;
      p.hurt(p.damageSources().mobAttack(zombie), 1);
    }
    h.assertTrue(zombie.getHealth() == 988, "Shock ignored internal cooldown");
    h.succeed();
  }
}
