package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CyberpsychoGameTests {
  private static Cyberpsycho psycho(GameTestHelper h, int rank) {
    var c = CityContent.CYBERPSYCHO.get().create(h.getLevel());
    c.configure(rank);
    var p = h.absolutePos(new BlockPos(2, 90, 2));
    c.moveTo(p.getX(), p.getY(), p.getZ(), 0, 0);
    c.setNoGravity(true);
    h.getLevel().addFreshEntity(c);
    return c;
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 60)
  public static void realInstalledCostsDetermineEveryContractTierAndSockets(GameTestHelper h) {
    int[] costs = {162, 187, 215, 250, 270, 278};
    for (int rank = 1; rank <= 6; rank++) {
      var plan = CyberpsychoLoadouts.forRank(rank);
      var inv = plan.inventory();
      h.assertTrue(
          inv.capacityUsed() == costs[rank - 1] && plan.rank() == rank,
          "Real capacity mismatches contract band");
      int count = 0;
      for (int s = 0; s < inv.getSlots(); s++) {
        var stack = inv.getStackInSlot(s);
        if (stack.isEmpty()) continue;
        count++;
        h.assertTrue(
            inv.isItemValid(s, stack) && stack.getItem() instanceof AugmentItem a && !a.iconic(),
            "Invalid or iconic equipment");
      }
      h.assertTrue(count == plan.implants().size(), "Lost socket equipment");
      var offer =
          new BountyRules.Offer(
              UUID.randomUUID(),
              1,
              rank,
              0,
              0,
              "Test",
              "cybernetics2077:cyberpsycho",
              BountyRules.Kind.CYBERPSYCHO);
      var dossier = BountyContracts.offerTag(offer);
      h.assertTrue(
          dossier.getInt("capacity") == inv.capacityUsed()
              && dossier.getInt("baseCapacity") == 150
              && dossier.getInt("rank") == CyberpsychoRules.rank(inv.capacityUsed()),
          "Offer did not derive rank from installed cost");
    }
    h.succeed();
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 80)
  public static void nativeAiActivatesBerserkAndBiomonitorSpendsRealPumpCharges(GameTestHelper h) {
    var c = psycho(h, 3);
    var victim = h.spawn(EntityType.COW, new BlockPos(4, 90, 2));
    victim.setNoAi(true);
    victim.setNoGravity(true);
    c.setTarget(victim);
    c.setHealth(c.getMaxHealth() * .3f);
    float before = c.getHealth();
    h.runAfterDelay(
        2,
        () -> {
          h.assertTrue(
              c.berserk() && c.systems().getLong("berserkReady") > c.now(),
              "No automatic OS activation");
          h.assertTrue(
              c.getHealth() > before
                  && c.systems().getInt("pumpCharges") == 1
                  && c.systems().getLong("pumpNext") > c.now(),
              "Biomonitor did not spend pump charge");
          h.assertTrue(
              c.getAttributeValue(Attributes.MOVEMENT_SPEED) > .27 * 1.2,
              "Adrenaline did not activate");
          c.setTarget(null);
          c.setNoAi(true);
          c.discard();
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 80)
  public static void reinforcedTendonsExecutesSecondAirborneImpulse(GameTestHelper h) {
    for (int x = 1; x <= 12; x++)
      for (int z = 1; z <= 4; z++)
        h.setBlock(new BlockPos(x, 89, z), net.minecraft.world.level.block.Blocks.STONE);
    var c = psycho(h, 2);
    c.setNoGravity(false);
    c.setOnGround(true);
    var victim = h.spawn(EntityType.COW, new BlockPos(9, 90, 2));
    victim.setNoAi(true);
    c.setTarget(victim);
    h.runAfterDelay(
        9,
        () -> {
          h.assertTrue(
              c.systems().getLong("jumpReady") > c.now()
                  && c.systems().getLong("lastDoubleJump") > 0
                  && c.getDeltaMovement().y > 0,
              "No real second airborne jump: systems="
                  + c.systems()
                  + " pos="
                  + c.position()
                  + " velocity="
                  + c.getDeltaMovement()
                  + " ground="
                  + c.onGround()
                  + " target="
                  + c.getTarget()
                  + " sight="
                  + c.hasLineOfSight(victim));
          c.discard();
          victim.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 60)
  public static void pumpChargesRechargeAndSelfIceBlocksOnlyOnceUntilReady(GameTestHelper h) {
    var c = psycho(h, 4);
    c.setHealth(c.getMaxHealth() * .1f);
    h.assertTrue(
        c.usePump() && c.usePump() && !c.usePump() && c.systems().getInt("pumpCharges") == 0,
        "Pump is not charge limited");
    c.systems().putLong("pumpNext", c.now());
    var attacker =
        new FakePlayer(
            h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "IceReview"));
    h.assertTrue(
        AugmentExpansion.blockHack(attacker, c) && !AugmentExpansion.blockHack(attacker, c),
        "Self ICE did not intercept real quickhack path/cooldown");
    h.runAfterDelay(
        2,
        () -> {
          h.assertTrue(c.systems().getInt("pumpCharges") >= 1, "Pump never recharged");
          c.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 60)
  public static void fatalHitUsesSecondHeartThenCooldownAllowsDeathAndSalvage(GameTestHelper h) {
    var c = psycho(h, 5);
    c.setHealth(1);
    c.hurt(h.getLevel().damageSources().generic(), 100000);
    h.assertTrue(
        c.isAlive()
            && c.getHealth() == c.getMaxHealth()
            && c.systems().getLong("heartReady") > c.now(),
        "Second Heart failed to stop fatal native damage");
    c.invulnerableTime = 0;
    c.hurt(h.getLevel().damageSources().generic(), 100000);
    h.assertTrue(!c.isAlive(), "Second Heart ignored cooldown");
    h.succeed();
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 60)
  public static void saveReloadPreservesMedicalCooldownsEquipmentAndMissingHealth(
      GameTestHelper h) {
    var c = psycho(h, 6);
    c.setHealth(c.getMaxHealth() * .2f);
    h.assertTrue(c.usePump(), "Pump failed");
    c.blockHack();
    float health = c.getHealth();
    double armor = c.getAttributeValue(Attributes.ARMOR);
    long ready = c.systems().getLong("iceReady");
    var saved = c.saveWithoutId(new CompoundTag());
    c.discard();
    var loaded = CityContent.CYBERPSYCHO.get().create(h.getLevel());
    loaded.load(saved);
    h.getLevel().addFreshEntity(loaded);
    loaded.refresh(false);
    h.assertTrue(
        loaded.capacity() == 278
            && loaded.rank() == 6
            && loaded.getHealth() == health
            && loaded.getAttributeValue(Attributes.ARMOR) == armor
            && loaded.systems().getInt("pumpCharges") == 1
            && loaded.systems().getLong("iceReady") == ready,
        "Reload reset health, sockets, cooldowns or stacked attributes");
    loaded.discard();
    h.succeed();
  }

  @GameTest(batch = "cyberpsychoSystems", template = "empty", timeoutTicks = 60)
  public static void nativeMeleeUsesWeaponDenseMarrowAndMicrogeneratorWithRearm(GameTestHelper h) {
    var c = psycho(h, 3);
    var victim = h.spawn(EntityType.COW, new BlockPos(3, 90, 2));
    victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
    victim.setHealth(1000);
    victim.setNoAi(true);
    c.setTarget(victim);
    h.assertTrue(
        c.doHurtTarget(victim) && victim.getHealth() < 990,
        "Native weapon/Dense Marrow attack ineffective");
    long ready = c.systems().getLong("microReady");
    h.assertTrue(ready > c.now(), "Microgenerator not used");
    CyberpsychoCombat.microgenerator(c, victim);
    h.assertTrue(c.systems().getLong("microReady") == ready, "Microgenerator ignored rearm");
    c.discard();
    victim.discard();
    h.succeed();
  }
}
