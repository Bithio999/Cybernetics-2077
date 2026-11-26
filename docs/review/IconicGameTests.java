package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;
import static dev.cybernetics2077.review.AugmentGameTests.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.*;
import dev.cybernetics2077.temporal.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class IconicGameTests {
  static int offer() {
    var all = CyberwareCatalogue.offers();
    for (int i = 0; i < all.size(); i++) if (all.get(i).iconic()) return i;
    throw new AssertionError("Missing Iconic offer");
  }

  static RipperdocMenu shop(GameTestHelper h, ServerPlayer p) {
    var npc = CityContent.RIPPERDOC.get().create(h.getLevel());
    npc.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(1, 30, 1))));
    npc.setNoAi(true);
    npc.setNoGravity(true);
    h.getLevel().addFreshEntity(npc);
    p.setPos(npc.position().add(1, 0, 0));
    p.getData(CityContent.ACCOUNT).reputation(50);
    p.getData(CityContent.ACCOUNT).credit(1_000_000);
    var menu = new RipperdocMenu(1, p.getInventory(), npc.getId());
    p.containerMenu = menu;
    for (int day = 0; day < 512 && !menu.stocked(offer()); day++)
      p.server.overworld().setDayTime(day * 24000L);
    h.assertTrue(menu.stocked(offer()), "Iconic never entered eligible daily stock");
    return menu;
  }

  @GameTest(template = "empty", batch = "iconic", timeoutTicks = 60)
  public static void iconicRequiresTierMatchedChipAndCannotUseSalvage(GameTestHelper h) {
    long time = h.getLevel().getServer().overworld().getDayTime();
    var p = player(h);
    try {
      var m = shop(h, p);
      long money = m.eddies();
      int i = offer();
      var o = CyberwareCatalogue.offers().get(i);
      h.assertTrue(
          o.tier() == 5 && o.price() == 75000 && o.requiredStreetCred() == 40,
          "Incorrect iconic cost/tier/reputation");
      h.assertTrue(i == CyberwareCatalogue.offers().size() - 1, "Existing offer indexes moved");
      h.assertTrue(
          !m.clickMenuButton(p, 1000 + i) && m.result() == 7, "Full-price bypass without chip");
      p.getInventory()
          .setItem(0, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(3).get(), 2));
      p.getInventory().setItem(1, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(4).get(), 2));
      h.assertTrue(!m.clickMenuButton(p, 1000 + i), "Wrong-tier chip accepted");
      p.getInventory()
          .setItem(2, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get(), 2));
      h.assertTrue(!m.clickMenuButton(p, 2000 + i), "Iconic accepted salvage discount packet");
      h.assertTrue(m.eddies() == money && m.requisitions(5) == 2, "Rejected buy charged payment");
      h.assertTrue(
          m.clickMenuButton(p, 1000 + i)
              && m.eddies() == money - 75000
              && m.requisitions(5) == 1
              && m.parts(5) == 2,
          "Correct payment not atomic");
      h.assertTrue(
          p.getInventory().contains(new ItemStack(o.item())), "Purchased Iconic not delivered");
    } finally {
      h.getLevel().getServer().overworld().setDayTime(time);
      p.discard();
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "iconic", timeoutTicks = 60)
  public static void inventoryFundsStockAndCredFailuresPreservePayment(GameTestHelper h) {
    long time = h.getLevel().getServer().overworld().getDayTime();
    var p = player(h);
    try {
      var m = shop(h, p);
      int i = offer();
      long money = m.eddies();
      for (int slot = 0; slot < 36; slot++)
        p.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
      p.getInventory()
          .setItem(0, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get(), 2));
      h.assertTrue(
          !m.clickMenuButton(p, 1000 + i)
              && m.result() == 2
              && m.requisitions(5) == 2
              && m.eddies() == money,
          "Full inventory charged payment");
      p.getInventory().getItem(0).setCount(1);
      h.assertTrue(
          m.clickMenuButton(p, 1000 + i) && m.requisitions(5) == 0,
          "Consumed singleton chip did not free an inventory slot");
      p.getInventory().clearContent();
      p.getInventory().setItem(0, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get()));
      p.getData(CityContent.ACCOUNT).spend((int) m.eddies());
      h.assertTrue(
          !m.clickMenuButton(p, 1000 + i) && m.result() == 1 && m.requisitions(5) == 1,
          "Insufficient funds consumed chip");
      p.getData(CityContent.ACCOUNT).credit(1_000_000);
      for (int day = 512; day < 1024 && m.stocked(i); day++)
        p.server.overworld().setDayTime(day * 24000L);
      h.assertTrue(
          !m.stocked(i)
              && !m.clickMenuButton(p, 1000 + i)
              && m.result() == 5
              && m.requisitions(5) == 1,
          "Stale stock accepted or consumed chip");
      var wallet = p.getData(CityContent.ACCOUNT);
      var saved = wallet.serializeNBT(p.registryAccess());
      saved.putInt("streetCred", 39);
      wallet.deserializeNBT(p.registryAccess(), saved);
      h.assertTrue(
          m.streetCred() == 39
              && !m.clickMenuButton(p, 1000 + i)
              && m.result() == 4
              && m.requisitions(5) == 1,
          "Reputation bypass accepted");
    } finally {
      h.getLevel().getServer().overworld().setDayTime(time);
      p.discard();
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "iconic", timeoutTicks = 60)
  public static void chromedLootCanDropAChipOnceAndRespectsMobLoot(GameTestHelper h) {
    var mob = EntityType.HUSK.create(h.getLevel());
    mob.setPos(h.absoluteVec(new Vec3(1, 40, 1)));
    EnhancedMobs.install(mob, List.of(Cybernetics2077.augment(SUBDERMAL_ARMOR, 5)));
    long seed = 0;
    for (; ; seed++) {
      var random = RandomSource.create(seed);
      random.nextDouble();
      if (random.nextDouble() < .05) break;
    }
    mob.getRandom().setSeed(seed);
    var drops = new ArrayList<ItemEntity>();
    EnhancedMobs.drops(
        new LivingDropsEvent(mob, h.getLevel().damageSources().genericKill(), drops, true));
    h.assertTrue(
        drops.stream().filter(e -> e.getItem().getItem() instanceof IconicRequisition).count() == 1
            && drops.stream()
                    .filter(e -> e.getItem().getItem() instanceof DamagedCyberneticPart)
                    .count()
                == 1,
        "Bonus chip replaced guaranteed salvage or did not drop");
    int size = drops.size();
    EnhancedMobs.drops(
        new LivingDropsEvent(mob, h.getLevel().damageSources().genericKill(), drops, true));
    h.assertTrue(drops.size() == size, "Repeated death duplicated chip");
    var ordinary = EntityType.HUSK.create(h.getLevel());
    EnhancedMobs.drops(
        new LivingDropsEvent(ordinary, h.getLevel().damageSources().genericKill(), drops, true));
    h.assertTrue(drops.size() == size, "Ordinary mob dropped chip");
    var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
    boolean before = rule.get();
    try {
      rule.set(false, h.getLevel().getServer());
      mob.getPersistentData().getCompound("cybernetics2077:enhanced").remove("salvaged");
      EnhancedMobs.drops(
          new LivingDropsEvent(mob, h.getLevel().damageSources().genericKill(), drops, true));
      h.assertTrue(drops.size() == size, "doMobLoot=false ignored for chip");
    } finally {
      rule.set(before, h.getLevel().getServer());
    }
    h.succeed();
  }

  static ServerPlayer apogee(GameTestHelper h) {
    var p = SandevistanGameTests.runner(h, 5);
    equip(p, MILITECH_APOGEE, 5, 0);
    return p;
  }

  @GameTest(template = "empty", batch = "iconicModel", timeoutTicks = 60)
  public static void apogeeAndDynalarUseRelativeStrengthAndSharedEnvironment(GameTestHelper h) {
    var a = apogee(h);
    var b = SandevistanGameTests.runner(h, 5);
    boolean pvp = a.server.isPvpAllowed();
    try {
      a.server.setPvpAllowed(true);
      b.setPos(a.position().add(0, 0, 3));
      AugmentRuntime.action(a, 13);
      AugmentRuntime.action(b, 13);
      Sandevistan.invalidate(a.level());
      h.assertTrue(
          Math.abs(Sandevistan.pace(a) - 1.25) < 1e-7
              && Math.abs(Sandevistan.pace(b) - .375) < 1e-7,
          "Different models did not compare proportionally");
      h.assertTrue(
          Math.abs(
                  Sandevistan.environmentPace(a.level(), a.getX(), a.getY(), a.getZ(), false)
                      - .1875)
              < 1e-7,
          "Time bubble ignored stronger model");
      equip(b, MILITECH_APOGEE, 5, 0);
      Sandevistan.tick(b);
      h.assertTrue(!Sandevistan.active(b), "Swapping same-tier model kept old activation");
      var bs = AugmentRuntime.state(b);
      bs.sandyCharge = 1;
      bs.sandyRearm = 0;
      AugmentRuntime.action(b, 13);
      Sandevistan.invalidate(a.level());
      h.assertTrue(Sandevistan.pace(a) == Sandevistan.pace(b), "Equal Iconics lost parity");
      a.server.setPvpAllowed(false);
      Sandevistan.stop(b, AugmentRuntime.now(b));
      Sandevistan.invalidate(a.level());
      h.assertTrue(Sandevistan.pace(b) == 1, "Iconic bypassed PvP protection");
    } finally {
      a.server.setPvpAllowed(pvp);
      SandevistanGameTests.cleanup(a, b);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "iconicBattery", timeoutTicks = 60)
  public static void modelBatteryPersistsAndAllowsSmallChargeWithoutReset(GameTestHelper h) {
    var p = apogee(h);
    try {
      AugmentRuntime.action(p, 13);
      var state = AugmentRuntime.state(p);
      long now = AugmentRuntime.now(p);
      h.assertTrue(state.sandyEnd - now == 120 && state.sandyApogee, "Incorrect Apogee duration");
      state.sandyEnd = now + 60;
      Sandevistan.stop(p, now);
      h.assertTrue(Math.abs(Sandevistan.charge(p) - .5) < 1e-7, "Early off lost battery");
      var save = state.serializeNBT(p.registryAccess());
      var restored = new AugmentState();
      restored.deserializeNBT(p.registryAccess(), save);
      h.assertTrue(restored.sandyApogee && restored.sandyCharge == .5, "Saved battery lost model");
      restored.deserializeNBT(p.registryAccess(), new CompoundTag());
      h.assertTrue(
          !restored.sandyApogee && restored.sandyProfile == 2, "Legacy battery migration broke");
      state.sandyClock = now;
      state.sandyRearm = 0;
      state.sandyCharge = .05;
      h.assertTrue(
          AugmentRuntime.action(p, 13) && state.sandyEnd - now == 6, "Low charge not usable");
      Sandevistan.stop(p, now);
      h.assertTrue(!AugmentRuntime.action(p, 13), "Rearm bypassed");
      state.sandyRearm = 0;
      state.sandyCharge = 0;
      state.sandyClock = now;
      h.assertTrue(!AugmentRuntime.action(p, 13), "Empty battery activated");
      state.sandyClock = now - 250;
      h.assertTrue(Math.abs(Sandevistan.charge(p) - .5) < 1e-7, "Incorrect Apogee recharge");
      equip(p, DYNALAR_SANDEVISTAN, 5, 0);
      h.assertTrue(Math.abs(Sandevistan.charge(p) - .5) < 1e-7, "Reinstallation reset charge");
    } finally {
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "iconicKills", timeoutTicks = 60)
  public static void realKillsRecoverChargeAndExhaustionWithCaps(GameTestHelper h) {
    var p = apogee(h);
    try {
      AugmentRuntime.action(p, 13);
      var state = AugmentRuntime.state(p);
      long now = AugmentRuntime.now(p);
      state.sandyEnd = now + 60;
      p.getFoodData().setExhaustion(2);
      var enemy = EntityType.HUSK.create(h.getLevel());
      enemy.setPos(p.position().add(0, 0, 2));
      enemy.setNoAi(true);
      h.getLevel().addFreshEntity(enemy);
      enemy.hurt(p.damageSources().playerAttack(p), 10000);
      h.assertTrue(
          state.sandyEnd == now + 72
              && Math.abs(p.getFoodData().getExhaustionLevel() - 1.12) < .001,
          "Confirmed enemy death did not recover battery/stamina equivalent");
      var cow = EntityType.COW.create(h.getLevel());
      cow.setPos(p.position().add(1, 0, 0));
      h.getLevel().addFreshEntity(cow);
      cow.hurt(p.damageSources().playerAttack(p), 10000);
      h.assertTrue(state.sandyEnd == now + 72, "Passive-animal farming restored duration");
      state.sandyEnd = now + 119;
      var next = EntityType.HUSK.create(h.getLevel());
      next.setPos(p.position().add(0, 0, 2));
      next.setNoAi(true);
      h.getLevel().addFreshEntity(next);
      next.hurt(p.damageSources().playerAttack(p), 10000);
      h.assertTrue(state.sandyEnd == now + 120, "Kill recovery exceeded full remaining battery");
    } finally {
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "iconicHeadshots", timeoutTicks = 60)
  public static void projectileHeadshotsUseImpactHeightAndDoNotLeak(GameTestHelper h) {
    var p = apogee(h);
    try {
      AugmentRuntime.action(p, 13);
      var target = EntityType.COW.create(h.getLevel());
      target.setPos(p.position().add(0, 0, 3));
      target
          .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
          .setBaseValue(100);
      target.setHealth(100);
      target.setNoAi(true);
      h.getLevel().addFreshEntity(target);
      var arrow = new Arrow(h.getLevel(), p, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
      var head =
          new EntityHitResult(
              target, new Vec3(target.getX(), target.getBoundingBox().maxY - .05, target.getZ()));
      NeoForge.EVENT_BUS.post(new ProjectileImpactEvent(arrow, head));
      p.getRandom().setSeed(0);
      target.hurt(p.damageSources().arrow(arrow, p), 10);
      h.assertTrue(Math.abs(target.getHealth() - 88) < .001, "Apogee headshot bonus missing");
      target.invulnerableTime = 0;
      p.getRandom().setSeed(0);
      target.hurt(p.damageSources().arrow(arrow, p), 10);
      h.assertTrue(
          Math.abs(target.getHealth() - 78) < .001,
          "Headshot marker leaked to another damage call");
      NeoForge.EVENT_BUS.post(
          new ProjectileImpactEvent(
              arrow, new EntityHitResult(target, target.position().add(0, .1, 0))));
      target.invulnerableTime = 0;
      p.getRandom().setSeed(0);
      target.hurt(p.damageSources().arrow(arrow, p), 10);
      h.assertTrue(Math.abs(target.getHealth() - 68) < .001, "Body hit received headshot bonus");
    } finally {
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "iconicInstall", timeoutTicks = 60)
  public static void iconicInstallsInOsSocketAndSyncsModel(GameTestHelper h) {
    var p = apogee(h);
    try {
      var item = Cybernetics2077.augment(MILITECH_APOGEE, 5);
      h.assertTrue(
          item.iconic()
              && item.capacityCost() == 44
              && item.category() == CyberwareCategory.OPERATING_SYSTEM,
          "Iconic metadata incorrect");
      var inv = p.getData(Cybernetics2077.CYBERWARE);
      h.assertTrue(
          inv.capacityUsed() == 44 && !inv.isItemValid(0, new ItemStack(item)),
          "Wrong socket/capacity");
      AugmentRuntime.action(p, 13);
      var tag = new CompoundTag();
      Sandevistan.snapshot(p, tag);
      h.assertTrue(
          tag.getBoolean("sandyIconic")
              && tag.getBoolean("sandyApogee")
              && tag.getInt("sandyTier") == 5
              && tag.getLong("sandyEnd") > 0,
          "Client model snapshot missing");
      h.assertTrue(
          Math.abs(AugmentExpansion.weaponCritChance(p, p, false) - .2) < .001
              && Math.abs(AugmentExpansion.critMultiplier(p) - 1.8) < .001,
          "Wrong critical package");
    } finally {
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }
}
