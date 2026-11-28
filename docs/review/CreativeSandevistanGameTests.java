package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.AugmentGameTests.*;

import dev.cybernetics2077.*;
import dev.cybernetics2077.city.CyberwareCatalogue;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.MobCyberwarePool;
import dev.cybernetics2077.temporal.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CreativeSandevistanGameTests {
  @GameTest(template = "empty", batch = "creativeSandevistan", timeoutTicks = 60)
  public static void creativeOnlyAcquisitionAndNativeInstallation(GameTestHelper h) {
    var item = Cybernetics2077.augment(CREATIVE_SANDEVISTAN, 5);
    h.assertTrue(CreativeContent.items().contains(item), "Missing creative-tab item");
    h.assertTrue(
        CyberwareCatalogue.offers().stream().noneMatch(o -> o.item() == item),
        "Creative-only implant leaked into ripperdoc stock");
    h.assertTrue(!MobCyberwarePool.pool().contains(item), "Creative implant leaked into mob pool");
    h.assertTrue(CyberwareCatalogue.offers().size() == 415, "Saved shop indices changed");
    var p = player(h);
    equip(p, CREATIVE_SANDEVISTAN, 5, 0);
    h.assertTrue(
        Sandevistan.tier(p) == 5 && Sandevistan.model(p) == SandevistanModel.CREATIVE,
        "Installed OS not recognized");
    h.assertTrue(
        p.getData(Cybernetics2077.CYBERWARE).capacityUsed() == 0,
        "Creative implant consumes capacity");
    h.succeed();
  }

  @GameTest(template = "empty", batch = "creativeSandevistan", timeoutTicks = 60)
  public static void hourLongBatteryFullyRechargesAndRearmsAfterTwoTicks(GameTestHelper h) {
    var p = SandevistanGameTests.runner(h, 5);
    equip(p, CREATIVE_SANDEVISTAN, 5, 0);
    h.assertTrue(Sandevistan.toggle(p), "Creative activation failed");
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    h.assertTrue(s.sandyEnd - now == 72000, "Battery does not last one hour");
    var snapshot = new CompoundTag();
    Sandevistan.snapshot(p, snapshot);
    h.assertTrue(snapshot.getDouble("sandyRechargeTicks") == 2, "HUD recharge is not two ticks");
    // Exercise real expiry and recharge without waiting an hour in the test server.
    s.sandyEnd = now;
    Sandevistan.tick(p);
    h.assertTrue(
        !Sandevistan.active(p) && Sandevistan.charge(p) == 0, "Expired battery was not emptied");
    h.assertTrue(!Sandevistan.toggle(p), "Rearm delay was skipped");
    h.runAfterDelay(
        1,
        () -> {
          h.assertTrue(
              Math.abs(Sandevistan.charge(p) - .5) < 1e-9, "Half charge not restored in one tick");
          h.assertTrue(!Sandevistan.toggle(p), "Reactivated before the two-tick rearm");
        });
    h.runAfterDelay(
        2,
        () -> {
          try {
            h.assertTrue(
                Sandevistan.charge(p) == 1 && Sandevistan.toggle(p),
                "Full battery/rearm not available after two ticks");
            h.assertTrue(
                s.sandyEnd - AugmentRuntime.now(p) == 72000,
                "Recharged activation shortened the battery");
          } finally {
            SandevistanGameTests.cleanup(p);
          }
          h.succeed();
        });
  }

  @GameTest(template = "empty", batch = "creativeSandevistan", timeoutTicks = 60)
  public static void extremeSlowPropagatesThroughRealFieldEntityBudgetAndSnapshot(
      GameTestHelper h) {
    var p = SandevistanGameTests.runner(h, 5);
    var mob = EntityType.ZOMBIE.create(h.getLevel());
    try {
      equip(p, CREATIVE_SANDEVISTAN, 5, 0);
      h.assertTrue(AugmentRuntime.action(p, 13), "Activation failed");
      mob.setPos(p.position().add(2, 0, 0));
      h.assertTrue(Math.abs(Sandevistan.strength(p) - 1000) < 1e-8, "Still clamped at 95%");
      double pace = Sandevistan.pace(mob);
      h.assertTrue(
          Math.abs(pace / Sandevistan.pace(p) - .001) < 1e-9,
          "Wrong real relative entity pace: " + pace);
      h.assertTrue(
          Math.abs(
                  Sandevistan.environmentPace(p.level(), mob.getX(), mob.getY(), mob.getZ(), false)
                      - pace)
              < 1e-9,
          "Environment is not using creative strength");
      int[] calls = {0};
      for (int i = 0; i < 800; i++) Sandevistan.tickEntity(mob, () -> calls[0]++);
      h.assertTrue(calls[0] == 1, "Mob budget clamped or lost virtual time: " + calls[0]);
      h.assertTrue(
          Math.abs(Sandevistan.weaponPace(p) - .00125) < 1e-9,
          "Gun mechanisms ignored extreme world pace");
      var tag = new CompoundTag();
      Sandevistan.snapshot(p, tag);
      h.assertTrue(
          tag.getInt("sandyModel") == SandevistanModel.CREATIVE.ordinal(),
          "Client receives wrong model");
      var saved = new AugmentState();
      saved.deserializeNBT(
          p.registryAccess(), AugmentRuntime.state(p).serializeNBT(p.registryAccess()));
      h.assertTrue(
          Sandevistan.profile(saved) == SandevistanModel.CREATIVE, "Battery profile not persisted");
      p.getData(Cybernetics2077.CYBERWARE)
          .setStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), ItemStack.EMPTY);
      Sandevistan.tick(p);
      h.assertTrue(
          !Sandevistan.active(p) && Sandevistan.pace(mob) == 1,
          "Uninstall left an extreme time bubble behind");
    } finally {
      mob.discard();
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }
}
