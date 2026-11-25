package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.AugmentGameTests.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class SandevistanGameTests {
  static ServerPlayer runner(GameTestHelper h, int tier) {
    var p = player(h);
    p.setPos(p.getX(), 180, p.getZ());
    equip(p, DYNALAR_SANDEVISTAN, tier, 0);
    h.getLevel().addNewPlayer(p);
    return p;
  }

  static void cleanup(ServerPlayer... players) {
    for (var p : players) {
      Sandevistan.stop(p, AugmentRuntime.now(p));
      p.discard();
      Sandevistan.invalidate(p.level());
    }
  }

  @GameTest(template = "empty", batch = "sandevistan", timeoutTicks = 60)
  public static void batteryToggleRearmAndSavedRecharge(GameTestHelper h) {
    var p = runner(h, 5);
    try {
      h.assertTrue(AugmentRuntime.action(p, 13) && Sandevistan.active(p), "Activation failed");
      var s = AugmentRuntime.state(p);
      s.sandyEnd = AugmentRuntime.now(p) + 100;
      h.assertTrue(
          AugmentRuntime.action(p, 13)
              && !Sandevistan.active(p)
              && Math.abs(Sandevistan.charge(p) - .5) < .001,
          "Early off lost remaining charge");
      h.assertTrue(!AugmentRuntime.action(p, 13), "Rearm delay bypassed");
      s.sandyClock = AugmentRuntime.now(p) - 300;
      s.sandyRearm = 0;
      h.assertTrue(Sandevistan.charge(p) > .99, "Offline/server-time recharge failed");
      var saved = new AugmentState();
      saved.deserializeNBT(p.registryAccess(), s.serializeNBT(p.registryAccess()));
      h.assertTrue(
          saved.sandyClock == s.sandyClock && saved.sandyCharge == s.sandyCharge,
          "Battery not persisted");
    } finally {
      cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "sandevistan", timeoutTicks = 60)
  public static void expirationAndUnequippingRemoveRealModifiers(GameTestHelper h) {
    var p = runner(h, 5);
    try {
      double base = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
      AugmentRuntime.action(p, 13);
      h.assertTrue(
          Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - base * 1.25) < .0001,
          "Actual movement modifier missing");
      var s = AugmentRuntime.state(p);
      s.sandyEnd = AugmentRuntime.now(p) - 1;
      Sandevistan.tick(p);
      h.assertTrue(
          !Sandevistan.active(p) && p.getAttributeValue(Attributes.MOVEMENT_SPEED) == base,
          "Expiry leaked speed");
      s.sandyCharge = 1;
      s.sandyRearm = 0;
      AugmentRuntime.action(p, 13);
      p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, ItemStack.EMPTY);
      Sandevistan.tick(p);
      h.assertTrue(
          !Sandevistan.active(p) && p.getAttributeValue(Attributes.ATTACK_SPEED) == 4,
          "Uninstall leaked combat boost");
    } finally {
      cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "sandevistan", timeoutTicks = 60)
  public static void differentDynalarTiersHaveEqualTemporalStrength(GameTestHelper h) {
    var a = runner(h, 2);
    var b = runner(h, 5);
    boolean pvp = a.server.isPvpAllowed();
    try {
      a.server.setPvpAllowed(true);
      b.setPos(a.position().add(0, 0, 3));
      AugmentRuntime.action(a, 13);
      AugmentRuntime.action(b, 13);
      Sandevistan.invalidate(a.level());
      h.assertTrue(
          Sandevistan.strength(a) == 2
              && Sandevistan.strength(b) == 2
              && Sandevistan.pace(a) == Sandevistan.pace(b),
          "Tier upgrades changed slowing strength");
      Sandevistan.stop(b, AugmentRuntime.now(b));
      Sandevistan.invalidate(a.level());
      h.assertTrue(
          Math.abs(Sandevistan.pace(a) / Sandevistan.pace(b) - 2) < .0001,
          "Expired user did not lose temporal parity");
      a.server.setPvpAllowed(false);
      Sandevistan.invalidate(a.level());
      h.assertTrue(Sandevistan.pace(b) == 1, "PvP-off player was slowed");
    } finally {
      a.server.setPvpAllowed(pvp);
      cleanup(a, b);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "sandevistan", timeoutTicks = 60)
  public static void projectilesUseTheirOwnersPaceOnlyInsideTheField(GameTestHelper h) {
    var p = runner(h, 5);
    try {
      AugmentRuntime.action(p, 13);
      var own = EntityType.ARROW.create(p.serverLevel());
      own.setOwner(p);
      own.setPos(p.position().add(0, 0, 3));
      var enemy = EntityType.ARROW.create(p.serverLevel());
      enemy.setPos(p.position().add(0, 0, 3));
      h.assertTrue(
          Math.abs(Sandevistan.pace(own) / Sandevistan.pace(enemy) - 2) < .0001,
          "Projectile rates ignore owner acceleration");
      own.setPos(p.position().add(0, 0, -200));
      h.assertTrue(
          Sandevistan.pace(own) == 1, "Projectile retains acceleration outside encounters");
    } finally {
      cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "sandevistan", timeoutTicks = 80)
  public static void nativeMobAndProjectileTicksSlowWhileWorldTimeDoesNot(GameTestHelper h) {
    var p = runner(h, 5);
    AugmentRuntime.action(p, 13);
    var center = new net.minecraft.world.level.ChunkPos(p.blockPosition());
    for (int x = center.x - 1; x <= center.x + 1; x++)
      for (int z = center.z - 1; z <= center.z + 1; z++) h.getLevel().setChunkForced(x, z, true);
    h.startSequence()
        .thenWaitUntil(
            () ->
                h.assertTrue(
                    h.getLevel().isPositionEntityTicking(p.blockPosition()),
                    "Waiting for forced chunk to tick entities"))
        .thenExecute(
            () -> {
              var mob = EntityType.ZOMBIE.create(h.getLevel());
              mob.setPos(p.position().add(0, 0, 5));
              mob.setNoAi(true);
              mob.setNoGravity(true);
              h.getLevel().addFreshEntity(mob);
              var arrow = EntityType.ARROW.create(h.getLevel());
              arrow.setPos(p.position().add(2, 0, 5));
              arrow.setNoGravity(true);
              arrow.setDeltaMovement(0, 0, .1);
              h.getLevel().addFreshEntity(arrow);
              long start = h.getLevel().getGameTime();
              h.runAfterDelay(
                  20,
                  () -> {
                    try {
                      h.assertTrue(
                          mob.tickCount >= 11
                              && mob.tickCount <= 14
                              && arrow.tickCount >= 11
                              && arrow.tickCount <= 14,
                          "Native ticks not paced: " + mob.tickCount + "/" + arrow.tickCount);
                      h.assertTrue(h.getLevel().getGameTime() - start >= 20, "World clock slowed");
                      h.assertTrue(arrow.getZ() - p.getZ() < 7, "Projectile physics did not slow");
                    } finally {
                      mob.discard();
                      arrow.discard();
                      cleanup(p);
                      for (int x = center.x - 1; x <= center.x + 1; x++)
                        for (int z = center.z - 1; z <= center.z + 1; z++)
                          h.getLevel().setChunkForced(x, z, false);
                    }
                    h.succeed();
                  });
            });
  }

  @GameTest(template = "empty", batch = "sandevistan", timeoutTicks = 60)
  public static void hurtProtectionDoesNotStretchAndCriticalBonusesReachWeapons(GameTestHelper h) {
    var p = runner(h, 5);
    try {
      AugmentRuntime.action(p, 13);
      var zombie = EntityType.ZOMBIE.create(h.getLevel());
      zombie.setPos(p.position().add(0, 0, 3));
      zombie.invulnerableTime = 20;
      Sandevistan.tickEntity(
          zombie,
          () -> {
            zombie.tickCount++;
            zombie.invulnerableTime--;
          });
      h.assertTrue(zombie.invulnerableTime == 19, "Skipping AI extended hurt protection");
      var target = EntityType.COW.create(h.getLevel());
      target.setPos(p.position().add(0, 0, 3));
      h.assertTrue(
          AugmentExpansion.weaponCritChance(p, target, true) > .149f
              && AugmentExpansion.critMultiplier(p) > 1.72,
          "Sandy critical bonuses missing");
      h.assertTrue(AugmentRuntime.interval(p, 10) == 8, "Mantis attack recovery not accelerated");
    } finally {
      cleanup(p);
    }
    h.succeed();
  }
}
