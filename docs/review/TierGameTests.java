package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class TierGameTests {
  private static ItemStack loaded(int deckTier, int chipTier) {
    var deck = new ItemStack(Cybernetics2077.CYBERDECK_TIERS.get(deckTier - 1).get());
    var chips = NonNullList.withSize(CyberdeckItem.slots(deck), ItemStack.EMPTY);
    chips.set(0, new ItemStack(Cybernetics2077.SHORT_CIRCUIT_TIERS.get(chipTier - 1).get()));
    deck.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(chips));
    return deck;
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void everyTierInstallsIntoCorrectBodySystems(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var inv = p.getData(Cybernetics2077.CYBERWARE);
    for (int tier = 0; tier < 5; tier++) {
      for (int slot = 0; slot < inv.getSlots(); slot++) inv.setStackInSlot(slot, ItemStack.EMPTY);
      var menu = new CyberneticsBenchMenu(1, p.getInventory());
      var eye = Cybernetics2077.OPTICS_TIERS.get(tier).get();
      var deck = Cybernetics2077.CYBERDECK_TIERS.get(tier).get();
      var arms = Cybernetics2077.MANTIS_TIERS.get(tier).get();
      p.getInventory().setItem(0, new ItemStack(eye));
      p.getInventory().setItem(1, new ItemStack(deck));
      p.getInventory().setItem(2, new ItemStack(arms));
      for (int i = 0; i < 3; i++) menu.quickMoveStack(p, 48 + i);
      h.assertTrue(
          ScannerNetwork.installed(p)
              && ScannerNetwork.range(p) == eye.tier().scanRange()
              && CyberwareRuntime.bladeTier(p) == arms.tier()
              && menu.hasDeck(),
          "Tier failed installation: " + (tier + 1));
      h.assertTrue(
          menu.capacityUsed() == eye.capacityCost() + deck.capacityCost() + arms.capacityCost(),
          "Capacity mismatch");
      h.assertTrue(!menu.getSlot(5).mayPlace(new ItemStack(arms)), "Arms accepted into optics");
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void deckSocketsTierGuardsAndDuplicatePrograms(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    for (int tier = 1; tier <= 5; tier++) {
      var deck = loaded(tier, 1);
      p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, deck);
      var menu = new CyberdeckMenu(1, p.getInventory(), ContainerLevelAccess.NULL, deck);
      h.assertTrue(
          menu.slots.size() == CyberwareTier.of(tier).programSlots() + 36,
          "Wrong synchronized slot count");
      h.assertTrue(
          !menu.getSlot(1)
              .mayPlace(new ItemStack(Cybernetics2077.SHORT_CIRCUIT_TIERS.get(tier - 1).get())),
          "Cross-tier duplicate accepted");
      menu.getSlot(0).set(ItemStack.EMPTY);
      h.assertTrue(
          menu.getSlot(0)
              .mayPlace(new ItemStack(Cybernetics2077.SHORT_CIRCUIT_TIERS.get(tier - 1).get())),
          "Same-tier chip refused");
      if (tier < 5)
        h.assertTrue(
            !menu.getSlot(0)
                .mayPlace(new ItemStack(Cybernetics2077.SHORT_CIRCUIT_TIERS.get(tier).get())),
            "Higher-tier chip accepted");
      h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND)), "Non-chip accepted");
      menu.getSlot(menu.programSlots() - 1)
          .set(new ItemStack(Cybernetics2077.SHORT_CIRCUIT_TIERS.get(tier - 1).get()));
      var copy =
          ItemStack.parseOptional(
              p.registryAccess(), (net.minecraft.nbt.CompoundTag) deck.save(p.registryAccess()));
      h.assertTrue(
          CyberdeckItem.chip(copy, menu.programSlots() - 1).getItem()
              == Cybernetics2077.SHORT_CIRCUIT_TIERS.get(tier - 1).get(),
          "Last chip lost on persistence");
    }
    h.assertTrue(
        CyberdeckItem.quickhack(loaded(1, 5)) == null,
        "Imported over-tier chip bypassed runtime guard");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void smithingUpgradesPreserveProgramsAndCustomComponents(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var materials =
        new Item[] {Items.GOLD_BLOCK, Items.DIAMOND, Items.NETHERITE_INGOT, Items.NETHER_STAR};
    for (var family :
        java.util.List.of(
            Cybernetics2077.OPTICS_TIERS,
            Cybernetics2077.CYBERDECK_TIERS,
            Cybernetics2077.MANTIS_TIERS,
            Cybernetics2077.SHORT_CIRCUIT_TIERS)) {
      ItemStack base =
          family.get(0).get() instanceof CyberdeckItem
              ? loaded(1, 1)
              : new ItemStack(family.get(0).get());
      base.set(
          DataComponents.CUSTOM_NAME,
          net.minecraft.network.chat.Component.literal("Preserved serial"));
      for (int tier = 2; tier <= 5; tier++) {
        var input =
            new SmithingRecipeInput(
                new ItemStack(Items.REDSTONE), base, new ItemStack(materials[tier - 2]));
        var recipe =
            p.serverLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, p.level());
        h.assertTrue(recipe.isPresent(), "Missing upgrade recipe at tier " + tier);
        var result = recipe.orElseThrow().value().assemble(input, p.registryAccess());
        h.assertTrue(
            result.getItem() == family.get(tier - 1).get() && result.getCount() == 1,
            "Wrong upgrade output");
        h.assertTrue(
            base.get(DataComponents.CUSTOM_NAME).equals(result.get(DataComponents.CUSTOM_NAME)),
            "Upgrade lost custom name");
        if (result.getItem() instanceof CyberdeckItem)
          h.assertTrue(
              CyberdeckItem.has(result, Cybernetics2077.SHORT_CIRCUIT.get()),
              "Upgrade erased loaded chip");
        base = result;
      }
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void scanningRangeUsesInstalledOpticsTier(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    target.setPos(p.position().add(0, 0, 55));
    p.lookAt(
        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
    var inv = p.getData(Cybernetics2077.CYBERWARE);
    inv.setStackInSlot(5, new ItemStack(Cybernetics2077.OPTICS.get()));
    h.assertTrue(
        !ScannerNetwork.inspect(p, target.getId()).contains("id"), "Tier-one range exceeded");
    inv.setStackInSlot(5, new ItemStack(Cybernetics2077.OPTICS_TIERS.get(4).get()));
    h.assertTrue(
        ScannerNetwork.inspect(p, target.getId()).contains("id"),
        "Tier-five long-range scan failed");
    target.setPos(p.position().add(0, 0, 70));
    h.assertTrue(
        !ScannerNetwork.inspect(p, target.getId()).contains("id"), "Tier-five range exceeded");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void bladeDamageScalesAtEveryTierWithoutChangingContactRules(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var target = CombatGameTests.target(h, p);
    var s = CyberwareRuntime.state(p);
    s.deployed = true;
    for (int tier = 0; tier < 5; tier++) {
      p.getData(Cybernetics2077.CYBERWARE)
          .setStackInSlot(4, new ItemStack(Cybernetics2077.MANTIS_TIERS.get(tier).get()));
      for (int combo : new int[] {0, 2, 3}) {
        target.setHealth(100);
        target.invulnerableTime = 0;
        s.combo = combo;
        s.attack = p.serverLevel().getGameTime() - 8;
        s.pendingHit = true;
        CyberwareRuntime.tick(p);
        float expected = CombatRules.damage(combo) * CyberwareTier.of(tier + 1).bladeMultiplier();
        h.assertTrue(
            Math.abs(100 - target.getHealth() - expected) < .001,
            "Wrong Mantis damage: tier " + (tier + 1) + " combo " + combo);
      }
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void ramRegenerationAndCapacityFollowDeckTier(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    for (int level = 1; level <= 5; level++) {
      p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, loaded(level, 1));
      var s = CyberwareRuntime.state(p);
      s.ram = 0;
      for (int i = 0; i < 20; i++) CyberwareRuntime.tick(p);
      h.assertTrue(
          Math.abs(s.ram - CyberwareTier.of(level).ramPerSecond()) < .001,
          "Wrong RAM regeneration");
      s.ram = 100;
      CyberwareRuntime.tick(p);
      h.assertTrue(s.ram == CyberwareTier.of(level).ram(), "RAM exceeded installed deck capacity");
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void highTierUploadsUseRealCostsAndRefundOnlyTheirReservations(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    CombatGameTests.install(p);
    var target = CombatGameTests.target(h, p);
    var deck = loaded(5, 5);
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, deck);
    var s = CyberwareRuntime.state(p);
    s.ram = 12;
    h.assertTrue(
        CyberwareRuntime.queue(p, target.getId()) && CyberwareRuntime.queue(p, target.getId()),
        "Tier-five uploads failed");
    h.assertTrue(
        s.ram == 0 && !CyberwareRuntime.queue(p, target.getId()), "Tier-five cost not reserved");
    h.assertTrue(
        Math.abs(s.uploads.getFirst().damage() - 26.4f) < .001
            && s.uploads.getFirst().finish() - s.uploads.getFirst().start() == 12,
        "Deck bonus/upload speed incorrect");
    deck.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    CyberwareRuntime.tick(p);
    h.assertTrue(s.ram == 12 && s.uploads.isEmpty(), "Wrong variable-cost refund");
    CyberwareRuntime.tick(p);
    h.assertTrue(s.ram == 12, "Repeated refund inflated RAM");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void queueLimitAndDeckSwapInvalidateOldUploads(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    CombatGameTests.install(p);
    var target = CombatGameTests.target(h, p);
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, loaded(5, 1));
    var s = CyberwareRuntime.state(p);
    s.ram = 12;
    for (int i = 0; i < 4; i++)
      h.assertTrue(CyberwareRuntime.queue(p, target.getId()), "Queue ended early");
    h.assertTrue(s.ram == 4 && !CyberwareRuntime.queue(p, target.getId()), "Queue limit ignored");
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, loaded(1, 1));
    CyberwareRuntime.tick(p);
    h.assertTrue(
        s.uploads.isEmpty() && s.ram == 4 && target.getHealth() == 100,
        "Deck swap kept old uploads or exceeded new RAM capacity");
    h.succeed();
  }
}
