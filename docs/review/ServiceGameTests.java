package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.MobCyberwarePool;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class ServiceGameTests {
  @GameTest(template = "empty", timeoutTicks = 60)
  public static void reputationCapacityUpdatesOpenMenusAndInstallation(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var inv = p.getData(Cybernetics2077.CYBERWARE);
    // 70 installed; the 35-capacity implant crosses the original 100 limit.
    AugmentGameTests.equip(p, Augment.SECOND_HEART, 5, 0);
    AugmentGameTests.equip(p, Augment.PAIN_EDITOR, 5, 0);
    AugmentGameTests.equip(p, Augment.SELF_ICE, 5, 0);
    var bench = new CyberneticsBenchMenu(1, p.getInventory());
    var viewer = CyberwareMenu.server(2, p.getInventory());
    var force = new ItemStack(Cybernetics2077.augment(Augment.IMMOVABLE_FORCE, 5));
    int socket = CyberwareCategory.HANDS.firstSlot();
    h.assertTrue(
        inv.capacityUsed() == 70 && bench.capacityLimit() == 100 && viewer.capacityLimit() == 100,
        "Starting limit mismatch");
    p.getData(CityContent.ACCOUNT).reputation(9);
    h.assertTrue(!bench.fits(socket, force), "Partial reputation step increased capacity");
    p.getData(CityContent.ACCOUNT).reputation(1);
    h.assertTrue(
        bench.capacityLimit() == 110 && viewer.capacityLimit() == 110 && bench.fits(socket, force),
        "Open menus or placement ignored Street Cred milestone");
    p.getData(CityContent.ACCOUNT).reputation(40);
    h.assertTrue(
        bench.capacityLimit() == 150 && viewer.capacityLimit() == 150,
        "SC50 final limit incorrect");
    h.assertTrue(
        CyberwareCapacity.limit(250, 50) == 300
            && CyberwareCapacity.limit(Integer.MAX_VALUE, 50) == Integer.MAX_VALUE,
        "Configured base or overflow mishandled");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void temporalPaceDoesNotLoadMissingChunks(GameTestHelper h) {
    var pos = new net.minecraft.core.BlockPos(12000000, 80, 12000000);
    h.assertTrue(
        h.getLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null,
        "Expected unloaded fixture");
    h.assertTrue(
        dev.cybernetics2077.temporal.TemporalEnvironment.pace(h.getLevel(), pos) == 1,
        "Unloaded pace incorrect");
    h.assertTrue(
        h.getLevel().getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null,
        "Pace loaded a missing chunk");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void immovableForceIsIconicAndExcludedFromChromedPool(GameTestHelper h) {
    for (int tier = 2; tier <= 5; tier++) {
      var item = Cybernetics2077.augment(Augment.IMMOVABLE_FORCE, tier);
      var offer =
          CyberwareCatalogue.offers().stream()
              .filter(o -> o.item() == item)
              .findFirst()
              .orElseThrow();
      h.assertTrue(
          item.iconic()
              && offer.iconic()
              && offer.price() == 3000 * tier * tier
              && offer.requiredStreetCred() == (tier - 1) * 10,
          "Immovable Force classification or price incorrect");
      h.assertTrue(!MobCyberwarePool.pool().contains(item), "Iconic leaked into chromed mob pool");
    }
    h.succeed();
  }
}
