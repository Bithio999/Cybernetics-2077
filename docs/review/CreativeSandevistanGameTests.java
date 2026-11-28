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
