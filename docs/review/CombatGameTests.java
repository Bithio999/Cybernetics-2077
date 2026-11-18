package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CombatGameTests {
  static void install(ServerPlayer p) {
    var inv = p.getData(Cybernetics2077.CYBERWARE);
    inv.setStackInSlot(
        CyberwareCategory.FACE.firstSlot(), new ItemStack(Cybernetics2077.OPTICS.get()));
    inv.setStackInSlot(
        CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
        new ItemStack(Cybernetics2077.CYBERDECK.get()));
    inv.setStackInSlot(
        CyberwareCategory.ARMS.firstSlot(), new ItemStack(Cybernetics2077.MANTIS.get()));
  }

  static void chip(ServerPlayer p) {
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get()));
    var menu =
        new CyberdeckMenu(2, p.getInventory(), ContainerLevelAccess.NULL, CyberwareRuntime.deck(p));
    menu.quickMoveStack(p, 29);
  }

  static net.minecraft.world.entity.animal.Cow target(GameTestHelper h, ServerPlayer p) {
    var pos = h.absolutePos(new BlockPos(2, 20, 2));
    p.setPos(Vec3.atBottomCenterOf(pos));
    var mob = h.spawn(EntityType.COW, new BlockPos(2, 20, 5));
    mob.setNoAi(true);
    mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
        .setBaseValue(100);
    mob.setHealth(100);
    p.lookAt(
        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, mob.getEyePosition());
    return mob;
  }

  static void refreshEquipment(ServerPlayer p) {
    try {
      var method = LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");
      method.setAccessible(true);
      method.invoke(p);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @GameTest(template = "empty", timeoutTicks = 120)
  public static void selectedWeaponsAreInactiveAndRestoredAfterRetraction(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    install(p);
    p.getInventory().clearContent();
    p.getInventory().selected = 0;
    var mob = target(h, p);
    refreshEquipment(p);
    double bareDamage =
        p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    double bareSpeed =
        p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
    CyberwareRuntime.action(p, 0);
    var weapon = new ItemStack(Items.NETHERITE_SWORD);
    var enchants =
        p.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
    weapon.enchant(
        enchants.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 5);
    weapon.enchant(
        enchants.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT),
        2);
    var saved = weapon.copy();
    p.getInventory().setItem(0, weapon);
    p.getInventory().offhand.set(0, new ItemStack(Items.SHIELD));
    refreshEquipment(p);
    h.assertTrue(!CyberwareRuntime.emptyHands(p), "Activation check must see real inventory hands");
    h.assertTrue(
        p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty(),
        "Gameplay hands are not empty");
    h.assertTrue(
        p.damageSources().playerAttack(p).getWeaponItem().isEmpty(),
        "Damage source leaked weapon/enchantments");
    h.assertTrue(
        p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                == bareDamage
            && p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)
                == bareSpeed,
        "Held weapon attributes affected cyberarm combat");
    h.runAfterDelay(
        31,
        () -> {
          CyberwareRuntime.tick(p);
          h.assertTrue(CyberwareRuntime.state(p).deployed, "Item selection closed deployed claws");
          CyberwareRuntime.action(p, 1);
          h.runAfterDelay(
              5,
              () -> {
                CyberwareRuntime.tick(p);
                h.assertTrue(
                    Math.abs(mob.getHealth() - 90) < .001 && !mob.isOnFire(),
                    "Weapon affected Mantis damage or added fire");
                h.assertTrue(
                    ItemStack.matches(saved, p.getInventory().getSelected()),
                    "Weapon stack or durability changed");
                CyberwareRuntime.action(p, 0);
                h.assertTrue(
                    p.getMainHandItem().isEmpty(),
                    "Item returned before cyberarms finished closing");
                h.runAfterDelay(
                    31,
                    () -> {
                      refreshEquipment(p);
                      h.assertTrue(
                          p.getMainHandItem().is(Items.NETHERITE_SWORD)
                              && p.getOffhandItem().is(Items.SHIELD),
                          "Stored hands were not restored");
                      h.assertTrue(
                          p.getAttributeValue(
                                  net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                              > bareDamage,
                          "Weapon attributes did not return after retraction");
                      CyberwareRuntime.action(p, 0);
                      h.assertTrue(
                          !CyberwareRuntime.state(p).deployed,
                          "Virtual hands bypassed the real empty-hand activation rule");
                      h.succeed();
                    });
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void benchAcceptsDeckAndArmsInTheirOwnCategories(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var menu = new CyberneticsBenchMenu(1, p.getInventory());
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.CYBERDECK.get()));
    p.getInventory().setItem(1, new ItemStack(Cybernetics2077.MANTIS.get()));
    h.assertTrue(!menu.getSlot(5).mayPlace(p.getInventory().getItem(0)), "Deck accepted in face");
    menu.quickMoveStack(p, 48);
    menu.quickMoveStack(p, 49);
    h.assertTrue(
        menu.hasDeck() && CyberwareRuntime.claws(p) && menu.capacityUsed() == 23,
        "New implants failed to install or wrong capacity");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void chipStorageSurvivesDeckRemovalAndRejectsDuplicates(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    install(p);
    chip(p);
    var deck = CyberwareRuntime.deck(p);
    h.assertTrue(CyberdeckItem.has(deck, Cybernetics2077.SHORT_CIRCUIT.get()), "Chip not saved");
    var menu = new CyberdeckMenu(2, p.getInventory(), ContainerLevelAccess.NULL, deck);
    h.assertTrue(
        !menu.getSlot(1).mayPlace(new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get())),
        "Duplicate quickhack accepted");
    h.assertTrue(!menu.getSlot(1).mayPlace(new ItemStack(Items.DIAMOND)), "Non-chip accepted");
    var bench = new CyberneticsBenchMenu(1, p.getInventory());
    bench.quickMoveStack(p, CyberwareCategory.OPERATING_SYSTEM.firstSlot());
    h.assertTrue(!menu.stillValid(p), "Detached deck remains editable");
    var removed =
        p.getInventory().items.stream()
            .filter(i -> i.getItem() instanceof CyberdeckItem)
            .findFirst()
            .orElseThrow();
    h.assertTrue(
        CyberdeckItem.has(removed, Cybernetics2077.SHORT_CIRCUIT.get()),
        "Removing deck erased chip");
    var serialized = removed.save(p.registryAccess());
    var copy =
        ItemStack.parseOptional(p.registryAccess(), (net.minecraft.nbt.CompoundTag) serialized);
    h.assertTrue(
        CyberdeckItem.has(copy, Cybernetics2077.SHORT_CIRCUIT.get()),
        "Serialized deck lost its program");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 80)
  public static void hacksRequireOpticsDeckChipAimAndReserveBoundedRam(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var mob = target(h, p);
    h.assertTrue(!CyberwareRuntime.queue(p, mob.getId()), "Hack works without equipment");
    install(p);
    h.assertTrue(!CyberwareRuntime.queue(p, mob.getId()), "Hack works without chip");
    chip(p);
    p.setYRot(p.getYRot() + 180);
    h.assertTrue(!CyberwareRuntime.queue(p, mob.getId()), "Off-axis hack accepted");
    p.lookAt(
        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, mob.getEyePosition());
    h.assertTrue(CyberwareRuntime.queue(p, mob.getId()), "Valid hack rejected");
    h.assertTrue(
        Math.abs(CyberwareRuntime.state(p).ram - 2) < .001 && mob.getHealth() == 100,
        "RAM not reserved or upload hit instantly");
    h.assertTrue(
        CyberwareRuntime.queue(p, mob.getId()) && !CyberwareRuntime.queue(p, mob.getId()),
        "Queue/RAM bounds failed");
    for (int i = 0; i < 800; i++) CyberwareRuntime.tick(p);
    h.assertTrue(CyberwareRuntime.state(p).ram <= 4, "RAM regenerated above max");
    h.runAfterDelay(
        21,
        () -> {
          CyberwareRuntime.tick(p);
          h.assertTrue(
              Math.abs(mob.getHealth() - 92) < .001,
              "First uploaded hack did not deal exactly 8 damage");
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void removingDeckCancelsUploadsAndNeverDuplicatesChips(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    install(p);
    chip(p);
    var mob = target(h, p);
    h.assertTrue(CyberwareRuntime.queue(p, mob.getId()), "Queue setup failed");
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), ItemStack.EMPTY);
    CyberwareRuntime.tick(p);
    h.assertTrue(
        CyberwareRuntime.state(p).uploads.isEmpty() && CyberwareRuntime.state(p).ram == 4,
        "Removed deck left upload or failed refund");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void mantisRequiresInstallationEmptyHandsAndContactFrame(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var mob = target(h, p);
    CyberwareRuntime.action(p, 0);
    h.assertTrue(!CyberwareRuntime.state(p).deployed, "Uninstalled claws deployed");
    install(p);
    p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
    CyberwareRuntime.action(p, 0);
    h.assertTrue(!CyberwareRuntime.state(p).deployed, "Claws deployed over weapon");
    p.getInventory().clearContent();
    CyberwareRuntime.action(p, 0);
    h.assertTrue(CyberwareRuntime.state(p).deployed, "Equipped empty-hand claws did not deploy");
    CyberwareRuntime.action(p, 1);
    h.assertTrue(!CyberwareRuntime.state(p).pendingHit, "Attack during deployment allowed");
    h.runAfterDelay(
        31,
        () -> {
          CyberwareRuntime.action(p, 1);
          h.assertTrue(
              CyberwareRuntime.state(p).pendingHit && mob.getHealth() == 100,
              "Attack applied before contact");
          h.runAfterDelay(
              5,
              () -> {
                CyberwareRuntime.tick(p);
                h.assertTrue(
                    Math.abs(mob.getHealth() - 90) < .001,
                    "Light contact damage incorrect: " + mob.getHealth());
                p.getInventory().setItem(0, new ItemStack(Items.STICK));
                CyberwareRuntime.tick(p);
                h.assertTrue(
                    CyberwareRuntime.state(p).deployed, "Selecting an item retracted the blades");
                h.assertTrue(
                    p.getMainHandItem().isEmpty(), "Selected item leaked into cyberarm hand");
                h.succeed();
              });
        });
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void bladeRayCannotHitThroughSolidTerrain(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var mob = target(h, p);
    h.assertTrue(CyberwareRuntime.bladeTarget(p, 4) == mob, "Valid blade ray failed");
    for (int y = 20; y <= 23; y++)
      h.setBlock(new BlockPos(2, y, 3), net.minecraft.world.level.block.Blocks.STONE);
    h.assertTrue(CyberwareRuntime.bladeTarget(p, 4) == null, "Blade ray bypassed a wall");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void leapTargetRequiresAimRangeVisibilityAndCancelsOnUnequip(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    install(p);
    var mob = target(h, p);
    h.assertTrue(CyberwareRuntime.lungeTarget(p) == mob, "Visible aimed target was not acquired");
    p.setYRot(p.getYRot() + 90);
    h.assertTrue(
        CyberwareRuntime.lungeTarget(p) == null, "Leap acquired a target outside aim cone");
    p.lookAt(
        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, mob.getEyePosition());
    for (int y = 20; y <= 23; y++)
      h.setBlock(new BlockPos(2, y, 3), net.minecraft.world.level.block.Blocks.STONE);
    h.assertTrue(CyberwareRuntime.lungeTarget(p) == null, "Leap acquired through solid terrain");
    for (int y = 20; y <= 23; y++)
      h.setBlock(new BlockPos(2, y, 3), net.minecraft.world.level.block.Blocks.AIR);
    mob.setPos(mob.getX(), mob.getY(), mob.getZ() + 9);
    h.assertTrue(CyberwareRuntime.lungeTarget(p) == null, "Leap acquired beyond eight blocks");
    var s = CyberwareRuntime.state(p);
    s.deployed = true;
    s.lunge =
        new CyberwareRuntime.Lunge(
            mob.getUUID(),
            new LungePath(
                p.getX(), p.getY(), p.getZ(), mob.getX(), mob.getY(), mob.getZ(), .65, 10),
            p.serverLevel().getGameTime());
    p.getInventory().setItem(0, new ItemStack(Items.STICK));
    CyberwareRuntime.tick(p);
    h.assertTrue(s.lunge != null && s.deployed, "Selecting an item interrupted the leap");
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(CyberwareCategory.ARMS.firstSlot(), ItemStack.EMPTY);
    CyberwareRuntime.tick(p);
    h.assertTrue(
        s.lunge == null && !s.deployed && !s.pendingHit,
        "Removing the implant failed to cancel leap/contact");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void viewerOpensInstalledDeckAndRejectsMissingDeckAndCarriedStack(
      GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    var menu = CyberwareMenu.server(1, p.getInventory());
    h.assertTrue(!menu.clickMenuButton(p, 0), "Missing deck opened a chip editor");
    install(p);
    menu.setCarried(new ItemStack(Items.DIAMOND));
    h.assertTrue(!menu.clickMenuButton(p, 0), "Carried item was lost through menu transition");
    menu.setCarried(ItemStack.EMPTY);
    h.assertTrue(!menu.clickMenuButton(p, 99), "Unknown viewer button accepted");
    h.assertTrue(
        menu.clickMenuButton(p, 0), "Installed deck was not available from cyberware viewer");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void leapSurvivesDelayedMovementAndCompletionCancelsIt(GameTestHelper h) {
    var p = ImplantGameTests.player(h);
    install(p);
    p.getInventory().clearContent();
    var s = CyberwareRuntime.state(p);
    s.deployed = true;
    s.lunge =
        new CyberwareRuntime.Lunge(
            null,
            new LungePath(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ() + 6, .65, 7),
            p.serverLevel().getGameTime() - 20);
    CyberwareRuntime.tick(p);
    h.assertTrue(s.lunge != null, "Delayed client movement was cancelled before acknowledgement");
    var momentum = new Vec3(.2, .15, .8);
    p.setDeltaMovement(momentum);
    CyberwareRuntime.action(p, 4);
    CyberwareRuntime.tick(p);
    h.assertTrue(s.lunge == null && !s.pendingHit, "Client completion failed to finish leap");
    h.assertTrue(
        p.getDeltaMovement().distanceToSqr(momentum) < .000001,
        "Server erased momentum at the end of the leap");
    h.succeed();
  }
}
