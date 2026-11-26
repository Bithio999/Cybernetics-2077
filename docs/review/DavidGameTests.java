package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.AugmentGameTests.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class DavidGameTests {
  static int offer() {
    for (int i = 0; i < CyberwareCatalogue.offers().size(); i++)
      if (CyberwareCatalogue.offers().get(i).item()
          == Cybernetics2077.augment(DAVIDS_SANDEVISTAN, 5)) return i;
    throw new AssertionError("Missing David offer");
  }

  static RipperdocMenu shop(GameTestHelper h, ServerPlayer p) {
    var npc = CityContent.RIPPERDOC.get().create(h.getLevel());
    npc.setPos(p.position().add(1, 0, 0));
    npc.setNoAi(true);
    h.getLevel().addFreshEntity(npc);
    return new RipperdocMenu(1, p.getInventory(), npc.getId());
  }

  static void stock(ServerPlayer p, RipperdocMenu m) {
    for (int d = 0; d < 512 && !m.stocked(offer()); d++)
      p.server.overworld().setDayTime(d * 24000L);
    if (!m.stocked(offer())) throw new AssertionError("David never stocked");
  }

  static ServerPlayer david(GameTestHelper h) {
    var p = SandevistanGameTests.runner(h, 5);
    equip(p, DAVIDS_SANDEVISTAN, 5, 0);
    return p;
  }

  @GameTest(template = "empty", batch = "davidShop", timeoutTicks = 80)
  public static void credFiftyAndBothMaterialsRequiredForAtomicUpgrade(GameTestHelper h) {
    var p = player(h);
    long day = p.server.overworld().getDayTime();
    try {
      var wallet = p.getData(CityContent.ACCOUNT);
      wallet.reputation(49);
      wallet.credit(150000);
      var m = shop(h, p);
      int i = offer();
      var o = CyberwareCatalogue.offers().get(i);
      h.assertTrue(
          o.requiredStreetCred() == 50 && o.price() == 150000 && o.iconic(), "Wrong David offer");
      for (int d = 0; d < 12; d++) {
        p.server.overworld().setDayTime(d * 24000L);
        h.assertFalse(m.stocked(i), "David stocked below cred 50");
      }
      h.assertFalse(m.clickMenuButton(p, 1000 + i), "Forged purchase bypassed Street Cred");
      wallet.reputation(1);
      stock(p, m);
      h.assertFalse(m.clickMenuButton(p, 1000 + i), "Missing chip accepted");
      p.getInventory()
          .setItem(0, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get(), 2));
      h.assertFalse(m.clickMenuButton(p, 1000 + i), "Missing Apogee accepted");
      h.assertTrue(
          m.result() == 8 && wallet.eddies() == 150000 && m.requisitions(5) == 2,
          "Failed upgrade charged resources");
      equip(p, MILITECH_APOGEE, 5, 0);
      h.assertFalse(m.clickMenuButton(p, 1000 + i), "Consumed an equipped Apogee");
      for (int s = 1; s < 36; s++) p.getInventory().setItem(s, new ItemStack(Items.STONE, 64));
      p.getInventory().setItem(1, new ItemStack(Cybernetics2077.augment(MILITECH_APOGEE, 5)));
      h.assertFalse(m.clickMenuButton(p, 2000 + i), "Salvage bypassed Iconic upgrade");
      h.assertTrue(
          m.clickMenuButton(p, 1000 + i), "Apogee slot was not reused in a full inventory");
      h.assertTrue(
          wallet.eddies() == 0
              && m.requisitions(5) == 1
              && p.getInventory().getItem(1).is(o.item())
              && m.prerequisites(o) == 0,
          "Upgrade did not consume exactly one of each input");
    } finally {
      p.server.overworld().setDayTime(day);
      p.discard();
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "davidModel", timeoutTicks = 80)
  public static void davidRelativeTimeBatteryPersistenceAndModelSwap(GameTestHelper h) {
    var a = david(h);
    var b = IconicGameTests.apogee(h);
    boolean pvp = a.server.isPvpAllowed();
    try {
      a.server.setPvpAllowed(true);
      b.setPos(a.position().add(0, 0, 3));
      Sandevistan.toggle(a);
      Sandevistan.toggle(b);
      Sandevistan.invalidate(a.level());
      h.assertTrue(
          Math.abs(Sandevistan.pace(a) - 1.25) < 1e-7
              && Math.abs(Sandevistan.pace(b) - 5. / 12) < 1e-7,
          "David/Apogee relative clock wrong");
      h.assertTrue(
          Math.abs(
                  Sandevistan.environmentPace(a.level(), a.getX(), a.getY(), a.getZ(), false)
                      - .0625)
              < 1e-7,
          "David bubble did not slow the environment");
      var s = AugmentRuntime.state(a);
      long now = AugmentRuntime.now(a);
      h.assertTrue(
          s.sandyEnd - now == 200 && s.sandyDavid && !s.sandyApogee,
          "Wrong David battery identity/duration");
      var tag = new CompoundTag();
      Sandevistan.snapshot(a, tag);
      h.assertTrue(
          tag.getInt("sandyModel") == 2 && tag.getBoolean("sandyDavid"), "Missing client model");
      s.sandyEnd = now + 100;
      Sandevistan.stop(a, now);
      var restored = new AugmentState();
      restored.deserializeNBT(a.registryAccess(), s.serializeNBT(a.registryAccess()));
      h.assertTrue(
          restored.sandyDavid && Math.abs(restored.sandyCharge - .5) < 1e-8,
          "Saved David battery lost its profile");
      equip(a, MILITECH_APOGEE, 5, 0);
      h.assertTrue(Math.abs(Sandevistan.charge(a) - .5) < 1e-8, "Model swap refilled charge");
      restored.deserializeNBT(a.registryAccess(), new CompoundTag());
      h.assertTrue(!restored.sandyDavid && !restored.sandyApogee, "Legacy state became David");
    } finally {
      a.server.setPvpAllowed(pvp);
      SandevistanGameTests.cleanup(a, b);
    }
    h.succeed();
  }

  static void kill(
      GameTestHelper h, ServerPlayer p, EntityType<? extends net.minecraft.world.entity.Mob> type) {
    var enemy = type.create(h.getLevel());
    enemy.setPos(p.position().add(0, 0, 2));
    enemy.setNoAi(true);
    h.getLevel().addFreshEntity(enemy);
    enemy.hurt(p.damageSources().playerAttack(p), 10000);
  }

  @GameTest(template = "empty", batch = "davidKills", timeoutTicks = 80)
  public static void secondWindHealsOnHostileKillsWithCooldownAndBatteryCap(GameTestHelper h) {
    var p = david(h);
    Sandevistan.toggle(p);
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    s.sandyEnd = now + 100;
    p.getFoodData().setFoodLevel(10);
    p.setHealth(10);
    float healed = 10 + p.getMaxHealth() * .1f;
    kill(h, p, EntityType.HUSK);
    h.assertTrue(
        Math.abs(p.getHealth() - healed) < .001 && s.sandyEnd == now + 120,
        "Kill recovery did not apply");
    kill(h, p, EntityType.HUSK);
    h.assertTrue(
        Math.abs(p.getHealth() - healed) < .001 && s.sandyEnd == now + 140,
        "Second Wind cooldown ignored");
    kill(h, p, EntityType.COW);
    h.assertTrue(
        Math.abs(p.getHealth() - healed) < .001 && s.sandyEnd == now + 140,
        "Passive animals granted recovery");
    s.sandyEnd = now + 199;
    kill(h, p, EntityType.HUSK);
    h.assertTrue(s.sandyEnd == now + 200, "Recovery exceeded a full battery");
    var load = new AugmentState();
    load.deserializeNBT(p.registryAccess(), s.serializeNBT(p.registryAccess()));
    h.assertTrue(load.sandyHealReady == s.sandyHealReady, "Healing cooldown not saved");
    h.runAfterDelay(
        21,
        () -> {
          try {
            kill(h, p, EntityType.HUSK);
            h.assertTrue(
                Math.abs(p.getHealth() - (healed + p.getMaxHealth() * .1f)) < .001,
                "Second Wind did not recover after cooldown");
            h.succeed();
          } finally {
            SandevistanGameTests.cleanup(p);
          }
        });
  }
}
