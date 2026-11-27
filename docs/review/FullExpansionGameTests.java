package dev.cybernetics2077.review;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.mob.*;
import dev.cybernetics2077.temporal.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class FullExpansionGameTests {
  private static void equip(ServerPlayer p, Augment a, int tier) {
    AugmentGameTests.equip(p, a, tier, 0);
  }

  private static ItemStack programmed(CyberdeckItem.Model model, int tier, QuickhackKind kind) {
    var stack = new ItemStack(Cybernetics2077.deck(model, tier));
    var list =
        NonNullList.withSize(
            tier == 5 ? 8 : CyberwareTier.of(tier).programSlots(), ItemStack.EMPTY);
    list.set(0, new ItemStack(Cybernetics2077.quickhack(kind, tier)));
    stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
    return stack;
  }

  @GameTest(batch = "fullCatalogue", template = "empty", timeoutTicks = 80)
  public static void currentCatalogueIsCompleteAndRuntimeDescriptionsExportToWiki(GameTestHelper h)
      throws Exception {
    var rows = new ArrayList<Map<String, Object>>();
    int variants = 0;
    for (var a : Augment.values())
      if (ExpansionImplants.added(a)) {
        var tiers = new ArrayList<Map<String, Object>>();
        for (int t = a.firstTier; t <= 5; t++) {
          var item = Cybernetics2077.augment(a, t);
          variants++;
          h.assertTrue(
              !a.details(t).isEmpty()
                  && item.capacityCost() == a.capacity
                  && item.iconic() == a.iconic(),
              "Incomplete implant metadata");
          var price =
              CyberwareCatalogue.offers().stream()
                  .filter(o -> o.item() == item)
                  .findFirst()
                  .orElseThrow();
          tiers.add(
              Map.of(
                  "tier",
                  t,
                  "effects",
                  a.details(t),
                  "price",
                  price.price(),
                  "streetCred",
                  price.requiredStreetCred()));
        }
        rows.add(
            Map.of(
                "id",
                a.id,
                "name",
                net.minecraft.network.chat.Component.translatable(a.translation()).getString(),
                "category",
                a.category.id,
                "firstTier",
                a.firstTier,
                "capacity",
                a.capacity,
                "iconic",
                a.iconic(),
                "tiers",
                tiers));
      }
    h.assertTrue(
        rows.size() == 60 && variants == 193 && CyberwareCatalogue.offers().size() == 415,
        "Catalogue coverage incomplete");
    h.assertTrue(
        CyberwareCatalogue.offers().get(74).item() == Cybernetics2077.RAVEN_TIERS.getFirst().get()
            && CyberwareCatalogue.offers().get(197).item()
                == Cybernetics2077.augment(MILITECH_APOGEE, 5),
        "Saved offer prefix moved");
    java.nio.file.Files.writeString(
        java.nio.file.Path.of("/workspace/expansion-catalogue.json"),
        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(rows));
    h.succeed();
  }

  @GameTest(batch = "npcUpload", template = "empty", timeoutTicks = 100)
  public static void ordinaryChromedMobUploadsRealInstalledProgramAndSpendsRam(GameTestHelper h) {
    var m = CyberpsychoGameTests.actor(h, SUBDERMAL_ARMOR);
    var deck = programmed(CyberdeckItem.Model.PARALINE, 3, QuickhackKind.SHORT_CIRCUIT);
    m.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), deck);
    MobImplants.initialize(m);
    ExpansionRuntime.ram(m, ExpansionRuntime.maxRam(m));
    var p = AugmentGameTests.player(h);
    p.setPos(m.position().add(20, 0, 0));
    h.getLevel().addNewPlayer(p);
    m.setTarget(p);
    float hp = p.getHealth();
    MobImplants.tick(m);
    h.assertTrue(
        ExpansionRuntime.ram(m) < ExpansionRuntime.maxRam(m)
            && p.getHealth() == hp
            && !MobImplants.state(m).getList("uploads", 10).isEmpty(),
        "NPC cast skipped reservation/upload delay");
    h.runAfterDelay(
        65,
        () -> {
          h.assertTrue(p.getHealth() < hp, "Real NPC upload never damaged its player target");
          m.discard();
          p.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "npcRelativeTime", template = "empty", timeoutTicks = 60)
  public static void mobSandevistanSlowsPlayersAndRespectsStrongerOpposingOperatingSystem(
      GameTestHelper h) {
    var m = CyberpsychoGameTests.actor(h, DYNALAR_SANDEVISTAN);
    var p = AugmentGameTests.player(h);
    p.setPos(m.position().add(8, 0, 0));
    h.getLevel().addNewPlayer(p);
    m.setTarget(p);
    MobImplants.tick(m);
    h.assertTrue(
        Sandevistan.active(m) && Math.abs(Sandevistan.pace(m) / Sandevistan.pace(p) - 2) < .001,
        "NPC field did not slow player relatively");
    equip(p, DAVIDS_SANDEVISTAN, 5);
    h.assertTrue(Sandevistan.toggle(p), "Opposing Sandy failed");
    h.assertTrue(
        Sandevistan.pace(m) < .13 && Sandevistan.playerPace(p) > 1,
        "Stronger player Sandy did not overtake NPC field");
    Sandevistan.stop(p, p.level().getGameTime());
    m.discard();
    p.discard();
    h.succeed();
  }

  @GameTest(batch = "newControls", template = "empty", timeoutTicks = 60)
  public static void weaponGlitchAndMalfunctionRespectIceAndDisableActualEquipment(
      GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    var m = CyberpsychoGameTests.actor(h, SUBDERMAL_ARMOR, SELF_ICE, DYNALAR_SANDEVISTAN);
    var deck = programmed(CyberdeckItem.Model.PARALINE, 5, QuickhackKind.CYBERWARE_MALFUNCTION);
    QuickhackEffects.land(
        p, m, Cybernetics2077.quickhack(QuickhackKind.CYBERWARE_MALFUNCTION, 5), 10, deck);
    h.assertTrue(
        AugmentRuntime.tier(m, SUBDERMAL_ARMOR) > 0 && !ExpansionRuntime.disabled(m),
        "First hack bypassed Self ICE");
    QuickhackEffects.land(
        p, m, Cybernetics2077.quickhack(QuickhackKind.CYBERWARE_MALFUNCTION, 5), 10, deck);
    h.assertTrue(
        ExpansionRuntime.disabled(m)
            && AugmentRuntime.tier(m, SUBDERMAL_ARMOR) == 0
            && Sandevistan.tier(m) == 0,
        "Malfunction did not disable real implants");
    QuickhackEffects.land(m, p, Cybernetics2077.quickhack(QuickhackKind.WEAPON_GLITCH, 5), 0, deck);
    h.assertTrue(
        ExpansionRuntime.jammed(p) && dev.cybernetics2077.compat.TacZCompat.blocked(p),
        "NPC weapon jam did not reach firing permission boundary");
    m.discard();
    h.succeed();
  }

  @GameTest(batch = "carryReserve", template = "empty", timeoutTicks = 60)
  public static void titaniumReserveEnforcesTierAndKeepsContentsAfterRemoval(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    equip(p, TITANIUM_BONES, 1);
    var menu = new CarryReserveMenu(1, p.getInventory());
    h.assertTrue(
        menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND))
            && !menu.getSlot(10).mayPlace(new ItemStack(Items.DIAMOND)),
        "Tier1 reserve exceeded nine slots");
    p.getData(Cybernetics2077.CARRY_RESERVE).setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.SKELETON.firstSlot(), ItemStack.EMPTY);
    h.assertTrue(
        !menu.stillValid(p)
            && p.getData(Cybernetics2077.CARRY_RESERVE).getStackInSlot(0).getCount() == 4,
        "Uninstall deleted reserve contents");
    equip(p, TITANIUM_BONES, 5);
    var upgraded = new CarryReserveMenu(2, p.getInventory());
    h.assertTrue(
        upgraded.getSlot(26).mayPlace(new ItemStack(Items.STONE)), "Tier5 reserve not fully open");
    h.succeed();
  }

  @GameTest(batch = "delayedWound", template = "empty", timeoutTicks = 140)
  public static void painducerDelaysRatherThanDeletesDamageAndCannotMitigateItTwice(
      GameTestHelper h) {
    var m = CyberpsychoGameTests.actor(h, PAINDUCER);
    float hp = m.getHealth();
    m.hurt(h.getLevel().damageSources().generic(), 20);
    float debt = ExpansionRuntime.extra(m).getFloat("woundDebt"), initialLoss = hp - m.getHealth();
    h.assertTrue(debt > 0 && initialLoss > 0, "No deferred wound");
    m.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.INTEGUMENTARY_SYSTEM.firstSlot(), ItemStack.EMPTY);
    for (int tick = 20; tick <= 120; tick += 20)
      h.runAfterDelay(tick, () -> ExpansionRuntime.tick(m));
    h.runAfterDelay(
        121,
        () -> {
          float remaining = ExpansionRuntime.extra(m).getFloat("woundDebt");
          h.assertTrue(
              remaining < .001 && Math.abs((hp - m.getHealth()) - initialLoss - debt) < .02,
              "Deferred wound vanished or recursively re-deferred: remaining="
                  + remaining
                  + ", loss="
                  + (hp - m.getHealth())
                  + ", expected="
                  + (initialLoss + debt));
          m.discard();
          h.succeed();
        });
  }

  @GameTest(batch = "tetratronic", template = "empty", timeoutTicks = 60)
  public static void deckIdentitiesKeepQueueAndDurationBonusesDistinct(GameTestHelper h) {
    var combat = Cybernetics2077.quickhack(QuickhackKind.SHORT_CIRCUIT, 5);
    var control = Cybernetics2077.quickhack(QuickhackKind.WEAPON_GLITCH, 5);
    var tetra = Cybernetics2077.deck(CyberdeckItem.Model.TETRATRONIC, 5);
    h.assertTrue(
        tetra.queuedMultiplier(combat, control) == 1.4f
            && tetra.queuedMultiplier(combat, combat) == 1
            && tetra.queuedMultiplier(control, combat) == 1,
        "Rippler queue bonus misplaced");
    var arasaka = Cybernetics2077.deck(CyberdeckItem.Model.ARASAKA, 5);
    var biotech = Cybernetics2077.deck(CyberdeckItem.Model.BIOTECH, 5);
    h.assertTrue(
        arasaka.cost(control) == control.cost() - 2
            && arasaka.durationMultiplier(control) == 1.25f
            && biotech.durationMultiplier(combat) == 1.6f,
        "Deck model identity lost");
    h.succeed();
  }

  @GameTest(batch = "newStatFamilies", template = "empty", timeoutTicks = 60)
  public static void conditionalArmorAndMedicalRechargeUseSharedActualEquipment(GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    equip(p, RARA_AVIS, 5);
    equip(p, SUBDERMAL_ARMOR, 5);
    AugmentRuntime.tick(p);
    ExpansionRuntime.tick(p);
    // Conditional refresh uses the world clock every ten ticks.
    h.runAfterDelay(
        (int) (10 - h.getLevel().getGameTime() % 10),
        () -> {
          ExpansionRuntime.tick(p);
          var x = ExpansionRuntime.extra(p);
          x.putLong("boosterEnd", h.getLevel().getGameTime() + 100);
          h.assertTrue(
              Math.abs(p.getAttributeValue(Attributes.ARMOR) - 7) < .01,
              "Rara Avis did not amplify physical armor");
          equip(p, COX_2, 5);
          var deck = programmed(CyberdeckItem.Model.PARALINE, 5, QuickhackKind.SHORT_CIRCUIT);
          p.getData(Cybernetics2077.CYBERWARE)
              .setStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), deck);
          h.assertTrue(ExpansionRuntime.maxRam(p) == 8, "COX-2 RAM penalty missing");
          h.succeed();
        });
  }

  @GameTest(batch = "compressor", template = "empty", timeoutTicks = 60)
  public static void compressorOccupiesOsAndAddsCorrectCapacityWithoutReplacingBase(
      GameTestHelper h) {
    var p = AugmentGameTests.player(h);
    int base = CyberwareCapacity.limit(p);
    equip(p, CHROME_COMPRESSOR, 5);
    h.assertTrue(
        CyberwareCapacity.limit(p) == base + 70
            && !(ExpansionRuntime.deck(p).getItem() instanceof CyberdeckItem),
        "Compressor did not consume OS/increase capacity");
    equip(p, RARA_AVIS, 5);
    equip(p, AXOLOTL, 5);
    equip(p, PERIPHERAL_INVERSE, 5);
    equip(p, COCKATRICE, 5);
    var menu = new dev.cybernetics2077.bench.CyberneticsBenchMenu(2, p.getInventory());
    if (menu.capacityUsed() > base)
      h.assertTrue(
          !menu.getSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot()).mayPickup(p),
          "Compressor could be removed while extra capacity was occupied");
    h.succeed();
  }

  @GameTest(batch = "npcNativeCadence", template = "empty", timeoutTicks = 80)
  public static void nativeMeleeRecoveryUsesInstalledSpeedInsteadOfInertAttribute(GameTestHelper h)
      throws Exception {
    var m = CyberpsychoGameTests.actor(h, MICROROTORS);
    var ordinary = EntityType.HUSK.create(h.getLevel());
    var method =
        net.minecraft.world.entity.ai.goal.MeleeAttackGoal.class.getDeclaredMethod(
            "getAttackInterval");
    method.setAccessible(true);
    var fast = new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(m, 1, false);
    var slow = new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(ordinary, 1, false);
    int quick = (int) method.invoke(fast), normal = (int) method.invoke(slow);
    h.assertTrue(
        quick < normal && quick == Math.max(6, (int) Math.ceil(normal / MobImplants.meleeRate(m))),
        "Native goal ignored equipped recovery");
    m.discard();
    h.succeed();
  }
}
