package dev.cybernetics2077.review;

import com.mojang.authlib.GameProfile;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class EnhancedMobGameTests {
  private static Husk mob(GameTestHelper h, int x) {
    var mob = EntityType.HUSK.create(h.getLevel());
    var pos = h.absolutePos(new BlockPos(x, 30, 1));
    mob.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    mob.setNoAi(true);
    mob.setNoGravity(true);
    h.getLevel().addFreshEntity(mob);
    return mob;
  }

  private static List<AugmentItem> six() {
    return List.of(
            Augment.SUBDERMAL_ARMOR,
            Augment.BIONIC_JOINTS,
            Augment.ADRENALINE_CONVERTER,
            Augment.BLOOD_PUMP,
            Augment.NEOFIBER,
            Augment.EPIMORPHIC_SKELETON)
        .stream()
        .map(a -> Cybernetics2077.augment(a, 5))
        .toList();
  }

  private static ServerPlayer player(GameTestHelper h) {
    return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "SalvageTest"));
  }

  private static RipperdocMenu shop(GameTestHelper h, ServerPlayer p) {
    var npc = CityContent.RIPPERDOC.get().create(h.getLevel());
    var pos = h.absolutePos(new BlockPos(1, 30, 1));
    npc.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    npc.setNoAi(true);
    npc.setNoGravity(true);
    h.getLevel().addFreshEntity(npc);
    p.setPos(npc.position().add(1, 0, 0));
    p.getData(CityContent.ACCOUNT).credit(1000000);
    var menu = new RipperdocMenu(1, p.getInventory(), npc.getId());
    p.containerMenu = menu;
    return menu;
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void rareNaturalSpawnsOnlyAndNoBosses(GameTestHelper h) {
    var m = mob(h, 1);
    h.assertTrue(
        Cybernetics2077.ENHANCED_SPAWN_CHANCE.get() == .02, "Expected rare 1-in-50 default");
    for (var reason : MobSpawnType.values()) {
      m.getPersistentData().remove("cybernetics2077:enhanced");
      if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.STRUCTURE) {
        NeoForge.EVENT_BUS.post(
            new FinalizeSpawnEvent(
                m,
                h.getLevel(),
                m.getX(),
                m.getY(),
                m.getZ(),
                h.getLevel().getCurrentDifficultyAt(m.blockPosition()),
                reason,
                null,
                null));
        h.assertTrue(
            !m.getPersistentData().getCompound("cybernetics2077:enhanced").getBoolean("pending"),
            "Non-natural spawn enhanced: " + reason);
      }
    }
    int marked = 0;
    m.getRandom().setSeed(18491);
    for (int i = 0; i < 10000; i++) {
      m.getPersistentData().remove("cybernetics2077:enhanced");
      NeoForge.EVENT_BUS.post(
          new FinalizeSpawnEvent(
              m,
              h.getLevel(),
              m.getX(),
              m.getY(),
              m.getZ(),
              h.getLevel().getCurrentDifficultyAt(m.blockPosition()),
              MobSpawnType.NATURAL,
              null,
              null));
      if (m.getPersistentData().getCompound("cybernetics2077:enhanced").getBoolean("pending"))
        marked++;
    }
    h.assertTrue(marked >= 130 && marked <= 270, "Natural rarity out of range: " + marked);
    h.assertTrue(
        !EnhancedMobs.eligible(EntityType.WITHER.create(h.getLevel()), MobSpawnType.NATURAL),
        "Bosses eligible");
    h.assertTrue(
        EnhancedMobs.eligible(EntityType.VINDICATOR.create(h.getLevel()), MobSpawnType.STRUCTURE),
        "Naturally generated mansion mobs excluded");
    m.getPersistentData().remove("cybernetics2077:enhanced");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void generatedLoadoutIsActualTieredCyberwareAndDoesNotReroll(GameTestHelper h) {
    var m = mob(h, 1);
    var pending = new CompoundTag();
    pending.putBoolean("pending", true);
    m.getPersistentData().put("cybernetics2077:enhanced", pending);
    EnhancedMobs.join(new EntityJoinLevelEvent(m, h.getLevel()));
    var original = m.getData(Cybernetics2077.CYBERWARE).serializeNBT(h.getLevel().registryAccess());
    var l = EnhancedMobs.loadout(m);
    h.assertTrue(
        l.count() >= 1 && l.count() <= 6 && l.averageTier() >= 1 && l.averageTier() <= 5,
        "Bad actual loadout");
    h.assertTrue(
        EnhancedMobs.tier(m, Augment.SUBDERMAL_ARMOR) > 0 && m.getArmorValue() > 2,
        "Generated armor not active");
    EnhancedMobs.join(new EntityJoinLevelEvent(m, h.getLevel()));
    h.assertTrue(
        original.equals(
            m.getData(Cybernetics2077.CYBERWARE).serializeNBT(h.getLevel().registryAccess())),
        "Loadout rerolled on join");
    h.assertTrue(!m.isPersistenceRequired(), "Named variants force permanent mob accumulation");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void statsLoadoutsHealthAndCooldownsSurviveSaveLoad(GameTestHelper h) {
    var m = mob(h, 1);
    EnhancedMobs.install(m, six());
    double max = m.getMaxHealth(), attack = m.getAttributeValue(Attributes.ATTACK_DAMAGE);
    h.assertTrue(
        max > 100 && attack > 9 && m.getArmorValue() >= 19, "Chassis/implants not applied");
    m.setHealth(17);
    var saved = m.saveWithoutId(new CompoundTag());
    var restored = EntityType.HUSK.create(h.getLevel());
    restored.load(saved);
    EnhancedMobs.join(new EntityJoinLevelEvent(restored, h.getLevel()));
    EnhancedMobs.join(new EntityJoinLevelEvent(restored, h.getLevel()));
    h.assertTrue(
        EnhancedMobs.loadout(restored).equals(new EnhancedMobs.Loadout(6, 5)),
        "Persistent implant inventory lost");
    h.assertTrue(
        restored.getMaxHealth() == max
            && restored.getHealth() == 17
            && restored.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack,
        "Reload refilled health or stacked modifiers");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void bloodPumpAndAdrenalineWorkInActualEntityTicks(GameTestHelper h) {
    var m = mob(h, 1);
    EnhancedMobs.install(m, six());
    m.setHealth(m.getMaxHealth() * .2f);
    float before = m.getHealth();
    double speed = m.getAttributeValue(Attributes.MOVEMENT_SPEED);
    var p = player(h);
    p.setPos(m.position().add(3, 0, 0));
    m.setTarget(p);
    h.runAfterDelay(
        23,
        () -> {
          h.assertTrue(m.getHealth() > before + m.getMaxHealth() * .3f, "Blood Pump did not heal");
          h.assertTrue(
              m.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed, "Adrenaline not active");
          var saved = m.saveWithoutId(new CompoundTag());
          var restored = EntityType.HUSK.create(h.getLevel());
          restored.load(saved);
          float low = restored.getMaxHealth() * .2f;
          restored.setHealth(low);
          restored.tickCount = Math.floorMod(-restored.getId(), 20);
          EnhancedMobs.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(restored));
          h.assertTrue(
              restored.getHealth() < low + restored.getMaxHealth() * .1f,
              "Reload reset Blood Pump cooldown");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void rangedAttacksReceiveChassisBonusWithoutChangingNormalArrows(GameTestHelper h) {
    var shooter = mob(h, 1);
    EnhancedMobs.install(shooter, List.of(Cybernetics2077.augment(Augment.SUBDERMAL_ARMOR, 5)));
    var cow = EntityType.COW.create(h.getLevel());
    cow.setPos(shooter.position().add(2, 0, 0));
    cow.setNoAi(true);
    cow.setNoGravity(true);
    h.getLevel().addFreshEntity(cow);
    var arrow = EntityType.ARROW.create(h.getLevel());
    arrow.setOwner(shooter);
    cow.hurt(h.getLevel().damageSources().arrow(arrow, shooter), 5);
    h.assertTrue(
        Math.abs(cow.getHealth() - 3) < .001,
        "Ranged chassis damage did not apply before mitigation");
    var normal = mob(h, 6);
    var other = EntityType.COW.create(h.getLevel());
    other.setPos(normal.position().add(2, 0, 0));
    other.setNoAi(true);
    other.setNoGravity(true);
    h.getLevel().addFreshEntity(other);
    arrow.setOwner(normal);
    other.hurt(h.getLevel().damageSources().arrow(arrow, normal), 5);
    h.assertTrue(Math.abs(other.getHealth() - 5) < .001, "Ordinary ranged damage was changed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void actualDeathDropsExactlyOnePartAndOrdinaryMobsDoNot(GameTestHelper h) {
    var m = mob(h, 1);
    EnhancedMobs.install(m, six());
    m.hurt(h.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
    var drops =
        h.getLevel()
            .getEntitiesOfClass(
                ItemEntity.class,
                m.getBoundingBox().inflate(2),
                e -> e.getItem().getItem() instanceof DamagedCyberneticPart);
    h.assertTrue(
        drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
        "Actual death missing single salvage drop");
    var list = new ArrayList<ItemEntity>();
    EnhancedMobs.drops(
        new LivingDropsEvent(m, h.getLevel().damageSources().genericKill(), list, true));
    h.assertTrue(list.isEmpty(), "Repeated death minted another part");
    var ordinary = mob(h, 6);
    EnhancedMobs.drops(
        new LivingDropsEvent(ordinary, h.getLevel().damageSources().genericKill(), list, true));
    h.assertTrue(list.isEmpty(), "Ordinary mob dropped salvage");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void salvageRespectsMobLootRuleAndConversionKeepsLoadout(GameTestHelper h)
      throws Exception {
    var m = mob(h, 1);
    EnhancedMobs.install(m, six());
    var before = h.getLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
    var drops = new ArrayList<ItemEntity>();
    try {
      h.getLevel()
          .getGameRules()
          .getRule(GameRules.RULE_DOMOBLOOT)
          .set(false, h.getLevel().getServer());
      EnhancedMobs.drops(
          new LivingDropsEvent(m, h.getLevel().damageSources().genericKill(), drops, true));
      h.assertTrue(drops.isEmpty(), "doMobLoot=false ignored");
    } finally {
      h.getLevel()
          .getGameRules()
          .getRule(GameRules.RULE_DOMOBLOOT)
          .set(before, h.getLevel().getServer());
    }
    var convert =
        net.minecraft.world.entity.monster.Zombie.class.getDeclaredMethod(
            "convertToZombieType", EntityType.class);
    convert.setAccessible(true);
    convert.invoke(m, EntityType.ZOMBIE);
    var converted =
        h.getLevel()
            .getEntitiesOfClass(
                net.minecraft.world.entity.monster.Zombie.class,
                m.getBoundingBox().inflate(.1),
                e -> e.getType() == EntityType.ZOMBIE);
    h.assertTrue(
        converted.size() == 1
            && EnhancedMobs.loadout(converted.getFirst()).equals(new EnhancedMobs.Loadout(6, 5)),
        "Conversion lost or rerolled implants");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void discountConsumesOneMatchingPartNormalPurchaseKeepsParts(GameTestHelper h) {
    var p = player(h);
    var m = shop(h, p);
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(0).get(), 3));
    long before = p.getData(CityContent.ACCOUNT).eddies();
    h.assertTrue(m.stocked(0) && m.clickMenuButton(p, 2000), "Valid salvage trade rejected");
    h.assertTrue(
        p.getData(CityContent.ACCOUNT).eddies() == before - 700 && m.parts(1) == 2,
        "Wrong salvage payment");
    h.assertTrue(
        m.clickMenuButton(p, 1000)
            && m.parts(1) == 2
            && p.getData(CityContent.ACCOUNT).eddies() == before - 1700,
        "Normal buy spent part or wrong price");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void wrongTierQuickhacksStockAndReputationCannotBypassTradeRules(GameTestHelper h) {
    var p = player(h);
    var m = shop(h, p);
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(1).get(), 8));
    long before = p.getData(CityContent.ACCOUNT).eddies();
    h.assertTrue(
        !m.clickMenuButton(p, 2000) && m.parts(2) == 8 && m.eddies() == before,
        "Wrong tier accepted");
    p.getInventory().setItem(1, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(0).get(), 8));
    var all = CyberwareCatalogue.offers();
    for (int i = 0; i < all.size(); i++) {
      var o = all.get(i);
      if (!m.stocked(i) || o.requiredStreetCred() > 0 || !(o.item() instanceof CyberwareItem))
        h.assertTrue(
            !m.clickMenuButton(p, 2000 + i), "Trade bypassed stock/cred/implant requirement");
    }
    h.assertTrue(
        m.parts(1) == 8 && m.parts(2) == 8 && m.eddies() == before,
        "Rejected trades charged player");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void fullInventoryAndInsufficientMoneyAreAtomicAndPartCanFreeSlot(
      GameTestHelper h) {
    var p = player(h);
    var m = shop(h, p);
    for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(0).get(), 2));
    long before = m.eddies();
    h.assertTrue(
        !m.clickMenuButton(p, 2000) && m.parts(1) == 2 && m.eddies() == before,
        "Full inventory consumed payment");
    p.getInventory().getItem(0).setCount(1);
    h.assertTrue(
        m.clickMenuButton(p, 2000)
            && p.getInventory().getItem(0).is(Cybernetics2077.OPTICS.get())
            && m.eddies() == before - 700,
        "Single part could not free purchase slot");
    p.getInventory().clearContent();
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(0).get(), 2));
    var a = p.getData(CityContent.ACCOUNT);
    a.spend((int) a.eddies());
    h.assertTrue(
        !m.clickMenuButton(p, 2000) && m.parts(1) == 2 && m.eddies() == 0,
        "No-money trade consumed part");
    h.succeed();
  }
}
