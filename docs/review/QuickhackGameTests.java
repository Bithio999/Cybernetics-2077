package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class QuickhackGameTests {
  static ItemStack loaded(boolean raven, int tier, QuickhackKind... kinds) {
    var deck =
        new ItemStack(
            raven
                ? Cybernetics2077.RAVEN_TIERS.get(tier - 3).get()
                : Cybernetics2077.CYBERDECK_TIERS.get(tier - 1).get());
    var chips = NonNullList.withSize(CyberdeckItem.slots(deck), ItemStack.EMPTY);
    for (int i = 0; i < kinds.length; i++)
      chips.set(
          i,
          new ItemStack(
              Cybernetics2077.quickhack(
                  kinds[i], Math.min(tier, Math.max(kinds[i].firstTier, 1)))));
    deck.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(chips));
    return deck;
  }

  static void install(ServerPlayer p, ItemStack deck) {
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(3, deck);
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(5, new ItemStack(Cybernetics2077.OPTICS_TIERS.get(4).get()));
    p.containerMenu = p.inventoryMenu;
    CyberwareRuntime.state(p).ram = AugmentRuntime.maxRam(p);
  }

  static Cow target(GameTestHelper h, ServerPlayer p) {
    var cow = h.spawn(EntityType.COW, new BlockPos(2, 30, 5));
    cow.setNoAi(true);
    cow.setNoGravity(true);
    cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
    cow.setHealth(1000);
    p.setPos(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 30, 2))));
    p.lookAt(
        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, cow.getEyePosition());
    return cow;
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void ravenTierProfilesAndCatalogueAreDistinct(GameTestHelper h) {
    for (int t = 3; t <= 5; t++) {
      var item = Cybernetics2077.RAVEN_TIERS.get(t - 3).get();
      var p = ImplantGameTests.player(h);
      install(p, new ItemStack(item));
      h.assertTrue(
          AugmentRuntime.maxRam(p) == t + 4
              && item.capacityCost() == 10 + 2 * t
              && item.damageBonus() == 0
              && item.relayTargets() == t - 2,
          "Wrong Raven profile");
      h.assertTrue(
          CyberwareCatalogue.offers().stream()
              .anyMatch(
                  o -> o.item() == item && o.requiredStreetCred() == (item.tier().level - 1) * 10),
          "Raven missing from gated daily pool");
    }
    for (var kind : QuickhackKind.values())
      for (int t = kind.firstTier; t <= 5; t++) {
        var item = Cybernetics2077.quickhack(kind, t);
        h.assertTrue(
            item.kind() == kind
                && item.tier().level == t
                && item.cost() > 0
                && CyberwareCatalogue.offers().stream().anyMatch(o -> o.item() == item),
            "Missing tiered program " + kind + t);
      }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void mixedLoadoutAndWireKeepCorrectDeckIdentity(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(true, 5, QuickhackKind.values());
    install(p, deck);
    h.assertTrue(CyberdeckItem.programs(deck).size() == 6, "Mixed loadout lost programs");
    var menu = new CyberdeckMenu(1, p.getInventory(), ContainerLevelAccess.NULL, deck);
    h.assertTrue(
        !menu.getSlot(6)
            .mayPlace(new ItemStack(Cybernetics2077.quickhack(QuickhackKind.OVERHEAT, 5))),
        "Duplicate kind across tiers accepted");
    var copy =
        ItemStack.parseOptional(
            p.registryAccess(), (net.minecraft.nbt.CompoundTag) deck.save(p.registryAccess()));
    h.assertTrue(CyberdeckItem.programs(copy).size() == 6, "Serialization erased distinct chips");
    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), p.registryAccess());
    try {
      ItemStack.STREAM_CODEC.encode(buffer, deck);
      var remote = new CyberdeckMenu(2, p.getInventory(), buffer);
      h.assertTrue(
          remote.cyberdeck().model() == CyberdeckItem.Model.RAVEN
              && remote.cyberdeck().ram() == 9
              && remote.programSlots() == 8,
          "Wire reconstructed Paraline instead of Raven");
    } finally {
      buffer.release();
    }
    var t3 = new ItemStack(Cybernetics2077.RAVEN_TIERS.get(0).get());
    install(p, t3);
    var lower = new CyberdeckMenu(3, p.getInventory(), ContainerLevelAccess.NULL, t3);
    h.assertTrue(
        !lower
            .getSlot(0)
            .mayPlace(new ItemStack(Cybernetics2077.quickhack(QuickhackKind.OVERHEAT, 4))),
        "Over-tier chip accepted");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void queueSelectsOwnedProgramsAndRefundsOnlyRemovedKinds(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(true, 5, QuickhackKind.SHORT_CIRCUIT, QuickhackKind.OVERHEAT);
    install(p, deck);
    var target = target(h, p);
    var state = CyberwareRuntime.state(p);
    h.assertTrue(
        !CyberwareRuntime.queue(p, target.getId(), -1)
            && !CyberwareRuntime.queue(p, target.getId(), QuickhackKind.CONTAGION.ordinal())
            && state.ram == 9,
        "Forged or unowned program charged RAM");
    h.assertTrue(
        CyberwareRuntime.queue(p, target.getId(), QuickhackKind.SHORT_CIRCUIT.ordinal())
            && CyberwareRuntime.queue(p, target.getId(), QuickhackKind.OVERHEAT.ordinal()),
        "Could not queue two distinct kinds");
    float before = state.ram;
    var chips = NonNullList.withSize(8, ItemStack.EMPTY);
    chips.set(0, new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get()));
    deck.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(chips));
    CyberwareRuntime.tick(p);
    h.assertTrue(
        state.uploads.size() == 1
            && state.uploads.getFirst().program().kind() == QuickhackKind.SHORT_CIRCUIT
            && Math.abs(state.ram - (before + 3 + .6f / 20)) < .001,
        "Removed program cancelled unrelated upload or lost RAM");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void ravenRelaysOnceAndLeavesBystandersUntouched(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(true, 3, QuickhackKind.SHORT_CIRCUIT);
    install(p, deck);
    var primary = target(h, p);
    var enemy = h.spawn(EntityType.HUSK, new BlockPos(4, 30, 5));
    enemy.setNoAi(true);
    enemy.setNoGravity(true);
    enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
    enemy.setHealth(1000);
    var bystander = h.spawn(EntityType.COW, new BlockPos(3, 30, 5));
    bystander.setNoAi(true);
    bystander.setNoGravity(true);
    QuickhackEffects.land(p, primary, Cybernetics2077.SHORT_CIRCUIT.get(), 8, deck);
    h.assertTrue(
        primary.getHealth() == 992
            && enemy.getHealth() == 996
            && bystander.getHealth() == bystander.getMaxHealth(),
        "Raven relay strength or target guard failed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void overheatDealsItsTotalOverTimeAndExpires(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(false, 1, QuickhackKind.OVERHEAT);
    install(p, deck);
    var target = target(h, p);
    QuickhackEffects.land(
        p, target, Cybernetics2077.quickhack(QuickhackKind.OVERHEAT, 1), 12, deck);
    h.assertTrue(target.getHealth() == 1000, "Overheat dealt its total instantly");
    h.runAtTickTime(25, () -> h.assertTrue(target.getHealth() == 997, "First thermal pulse wrong"));
    h.runAtTickTime(
        86,
        () -> {
          h.assertTrue(
              target.getHealth() == 988
                  && !QuickhackEffects.affected(target, QuickhackKind.OVERHEAT),
              "Thermal total or expiration wrong");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void controlHacksCoexistThenReleaseTheirTarget(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(true, 3, QuickhackKind.REBOOT_OPTICS, QuickhackKind.CRIPPLE_MOVEMENT);
    install(p, deck);
    var target = target(h, p);
    var mob = h.spawn(EntityType.HUSK, new BlockPos(4, 30, 5));
    mob.setNoAi(true);
    mob.setNoGravity(true);
    mob.setTarget(p);
    QuickhackEffects.land(
        p, mob, Cybernetics2077.quickhack(QuickhackKind.REBOOT_OPTICS, 1), 0, deck);
    mob.setTarget(p);
    h.assertTrue(mob.getTarget() == null, "Blinded mob reacquired target");
    QuickhackEffects.land(
        p, target, Cybernetics2077.quickhack(QuickhackKind.REBOOT_OPTICS, 1), 0, deck);
    QuickhackEffects.land(
        p, target, Cybernetics2077.quickhack(QuickhackKind.CRIPPLE_MOVEMENT, 2), 0, deck);
    target.setDeltaMovement(1, 1, 1);
    QuickhackEffects.tick(target);
    h.assertTrue(
        target.getDeltaMovement().equals(Vec3.ZERO)
            && QuickhackEffects.affected(target, QuickhackKind.REBOOT_OPTICS),
        "Root failed or replaced blindness");
    h.runAtTickTime(
        86,
        () -> {
          h.assertTrue(
              !QuickhackEffects.affected(target, QuickhackKind.REBOOT_OPTICS)
                  && !QuickhackEffects.affected(target, QuickhackKind.CRIPPLE_MOVEMENT),
              "Control did not expire");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void contagionSpreadsAndHeatDetonatesHighTierInfection(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(false, 5, QuickhackKind.CONTAGION, QuickhackKind.OVERHEAT);
    install(p, deck);
    var primary = target(h, p);
    var enemy = h.spawn(EntityType.HUSK, new BlockPos(4, 30, 5));
    enemy.setNoAi(true);
    enemy.setNoGravity(true);
    QuickhackEffects.land(
        p, primary, Cybernetics2077.quickhack(QuickhackKind.CONTAGION, 4), 24, deck);
    h.assertTrue(
        QuickhackEffects.affected(primary, QuickhackKind.CONTAGION)
            && QuickhackEffects.affected(enemy, QuickhackKind.CONTAGION),
        "Infection did not spread");
    QuickhackEffects.land(
        p, primary, Cybernetics2077.quickhack(QuickhackKind.OVERHEAT, 1), 12, deck);
    h.assertTrue(
        primary.getHealth() == 984
            && !QuickhackEffects.affected(primary, QuickhackKind.CONTAGION)
            && QuickhackEffects.affected(primary, QuickhackKind.OVERHEAT),
        "Contagion/Overheat detonation failed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void synapseUsesMissingRamAndSnapshotMatchesReservedDamage(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(false, 5, QuickhackKind.SYNAPSE_BURNOUT);
    var chips = NonNullList.withSize(8, ItemStack.EMPTY);
    chips.set(0, new ItemStack(Cybernetics2077.quickhack(QuickhackKind.SYNAPSE_BURNOUT, 5)));
    deck.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(chips));
    install(p, deck);
    var target = target(h, p);
    var program = Cybernetics2077.quickhack(QuickhackKind.SYNAPSE_BURNOUT, 5);
    float full = CyberwareRuntime.quickhackDamage(p, program);
    CyberwareRuntime.state(p).ram = 8;
    float exhausted = CyberwareRuntime.quickhackDamage(p, program);
    h.assertTrue(
        Math.abs(full - 77.76) < .01 && Math.abs(exhausted - 86.4) < .01,
        "Synapse RAM scaling wrong");
    h.assertTrue(
        CyberwareRuntime.queue(p, target.getId(), QuickhackKind.SYNAPSE_BURNOUT.ordinal())
            && Math.abs(CyberwareRuntime.state(p).uploads.getFirst().damage() - exhausted) < .001
            && CyberwareRuntime.state(p).ram == 0,
        "Synapse double-counted its cost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void rebootClearsBrainTargetsAndRavenRespectsWallsAndTeams(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var deck = loaded(true, 5, QuickhackKind.REBOOT_OPTICS, QuickhackKind.SHORT_CIRCUIT);
    install(p, deck);
    var primary = target(h, p);
    var piglin = h.spawn(EntityType.PIGLIN, new BlockPos(4, 30, 5));
    piglin.setNoAi(true);
    piglin.setNoGravity(true);
    piglin
        .getBrain()
        .setMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET, p);
    QuickhackEffects.land(
        p, piglin, Cybernetics2077.quickhack(QuickhackKind.REBOOT_OPTICS, 1), 0, deck);
    h.assertTrue(
        piglin
            .getBrain()
            .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
            .isEmpty(),
        "Blinded brain kept its attack memory");
    piglin.discard();
    var ally = h.spawn(EntityType.HUSK, new BlockPos(3, 30, 5));
    ally.setNoAi(true);
    ally.setNoGravity(true);
    var blocked = h.spawn(EntityType.HUSK, new BlockPos(2, 30, 9));
    blocked.setNoAi(true);
    blocked.setNoGravity(true);
    var board = p.getScoreboard();
    var team =
        board.addPlayerTeam("hack_" + java.util.UUID.randomUUID().toString().substring(0, 8));
    board.addPlayerToTeam(p.getScoreboardName(), team);
    board.addPlayerToTeam(ally.getScoreboardName(), team);
    for (int x = 0; x <= 5; x++)
      for (int y = 29; y <= 34; y++)
        h.setBlock(new BlockPos(x, y, 7), net.minecraft.world.level.block.Blocks.STONE);
    QuickhackEffects.land(p, primary, Cybernetics2077.SHORT_CIRCUIT.get(), 8, deck);
    h.assertTrue(
        ally.getHealth() == ally.getMaxHealth() && blocked.getHealth() == blocked.getMaxHealth(),
        "Raven hit an ally or relayed through a wall");
    board.removePlayerTeam(team);
    h.succeed();
  }
}
