package dev.cybernetics2077.review;

import com.mojang.authlib.GameProfile;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class ImplantGameTests {
  static ServerPlayer player(GameTestHelper h) {
    return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "ImplantTest"));
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void tierAndSlotDefinitions(GameTestHelper h) {
    var eye = Cybernetics2077.OPTICS.get();
    h.assertTrue(
        eye.tier() == CyberwareTier.ONE
            && eye.category() == CyberwareCategory.FACE
            && eye.capacityCost() == 4,
        "Scanner definition changed");
    for (int i = 0; i < 5; i++)
      h.assertTrue(CyberwareTier.values()[i].level == i + 1, "Noncontiguous tiers");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void benchInstallsAndRemovesWithoutDuplication(GameTestHelper h) {
    var p = player(h);
    p.getInventory().setItem(0, new ItemStack(Cybernetics2077.OPTICS.get()));
    var menu = new CyberneticsBenchMenu(1, p.getInventory());
    int face = CyberwareCategory.FACE.firstSlot();
    h.assertTrue(
        menu.quickMoveStack(p, 48).is(Cybernetics2077.OPTICS.get()),
        "Shift-click rejected implant");
    h.assertTrue(
        ScannerNetwork.installed(p)
            && menu.capacityUsed() == 4
            && p.getInventory().getItem(0).isEmpty(),
        "Implant was duplicated or not installed");
    menu.quickMoveStack(p, face);
    h.assertTrue(
        !ScannerNetwork.installed(p) && menu.capacityUsed() == 0, "Removal left effect active");
    int count = 0;
    for (var s : p.getInventory().items)
      if (s.is(Cybernetics2077.OPTICS.get())) count += s.getCount();
    h.assertTrue(count == 1, "Removal lost/duplicated implant");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void benchGuardsCategoryCapacityAndNormalItems(GameTestHelper h) {
    var p = player(h);
    var menu = new CyberneticsBenchMenu(1, p.getInventory());
    var eye = new ItemStack(Cybernetics2077.OPTICS.get());
    h.assertTrue(
        !menu.getSlot(0).mayPlace(eye) && menu.getSlot(5).mayPlace(eye),
        "Body-system guard failed");
    h.assertTrue(!menu.getSlot(5).mayPlace(new ItemStack(Items.DIAMOND)), "Normal item accepted");
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(0, ReviewItems.cortex());
    h.assertTrue(!menu.getSlot(5).mayPlace(eye), "Capacity guard failed");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void fullInventoryDoesNotLoseRemovedImplant(GameTestHelper h) {
    var p = player(h);
    var menu = new CyberneticsBenchMenu(1, p.getInventory());
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(5, new ItemStack(Cybernetics2077.OPTICS.get()));
    for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    h.assertTrue(
        menu.quickMoveStack(p, 5).isEmpty() && ScannerNetwork.installed(p),
        "Full inventory destroyed implant");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void benchValidityRequiresNearbyIntactWorkstation(GameTestHelper h) {
    var p = player(h);
    var pos = h.absolutePos(new BlockPos(0, 1, 0));
    p.serverLevel().setBlock(pos, Cybernetics2077.BENCH.get().defaultBlockState(), 3);
    p.setPos(Vec3.atCenterOf(pos));
    var menu =
        new CyberneticsBenchMenu(1, p.getInventory(), ContainerLevelAccess.create(p.level(), pos));
    h.assertTrue(menu.stillValid(p), "Valid bench rejected");
    p.setPos(Vec3.atCenterOf(pos).add(20, 0, 0));
    h.assertTrue(!menu.stillValid(p), "Remote access allowed");
    p.setPos(Vec3.atCenterOf(pos));
    p.serverLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    h.assertTrue(!menu.stillValid(p), "Broken bench remained usable");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void scansRequireInstalledEyeAimRangeAndLineOfSight(GameTestHelper h) {
    var p = player(h);
    var pos = h.absolutePos(new BlockPos(2, 30, 2));
    p.setPos(Vec3.atBottomCenterOf(pos));
    var targetChunk = h.absolutePos(new BlockPos(2, 30, 7));
    h.getLevel().setChunkForced(targetChunk.getX() >> 4, targetChunk.getZ() >> 4, true);
    h.getLevel().getChunkAt(targetChunk);
    var mob = h.spawn(EntityType.ZOMBIE, new BlockPos(2, 30, 7));
    mob.setNoAi(true);
    p.lookAt(
        net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, mob.getEyePosition());
    h.runAfterDelay(
        2,
        () -> {
          h.assertTrue(
              !ScannerNetwork.inspect(p, mob.getId()).contains("id"),
              "Scan worked without implant");
          p.getData(Cybernetics2077.CYBERWARE)
              .setStackInSlot(5, new ItemStack(Cybernetics2077.OPTICS.get()));
          h.assertTrue(
              ScannerNetwork.inspect(p, mob.getId()).contains("id"),
              "Valid scan failed: owned="
                  + ScannerNetwork.installed(p)
                  + " player="
                  + p.isAlive()
                  + " menu="
                  + (p.containerMenu == p.inventoryMenu)
                  + " found="
                  + (p.serverLevel().getEntity(mob.getId()) != null)
                  + " mob="
                  + mob.isAlive()
                  + " LOS="
                  + p.hasLineOfSight(mob)
                  + " eye="
                  + p.getEyePosition()
                  + " look="
                  + p.getLookAngle()
                  + " target="
                  + mob.position());
          p.setYRot(180);
          h.assertTrue(
              !ScannerNetwork.inspect(p, mob.getId()).contains("id"), "Off-axis scan allowed");
          p.lookAt(
              net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
              mob.getEyePosition());
          for (int y = 30; y <= 33; y++) h.setBlock(new BlockPos(2, y, 4), Blocks.STONE);
          h.assertTrue(
              !ScannerNetwork.inspect(p, mob.getId()).contains("id"), "Scan passed through wall");
          for (int y = 30; y <= 33; y++) h.setBlock(new BlockPos(2, y, 4), Blocks.AIR);
          mob.setPos(mob.position().add(0, 0, 40));
          p.lookAt(
              net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
              mob.getEyePosition());
          h.assertTrue(
              !ScannerNetwork.inspect(p, mob.getId()).contains("id"), "Out-of-range scan allowed");
          h.succeed();
        });
  }
}
