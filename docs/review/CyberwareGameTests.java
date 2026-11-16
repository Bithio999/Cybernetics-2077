package dev.cybernetics2077.review;

import com.mojang.authlib.GameProfile;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CyberwareGameTests {
  private static ServerPlayer player(GameTestHelper h) {
    return FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "CyberwareTest"));
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void categoryGuardsAndCapacity(GameTestHelper h) {
    var inventory = new CyberwareInventory();
    var stack = ReviewItems.cortex();
    h.assertTrue(
        inventory
                .insertItem(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), stack, false)
                .getCount()
            == 1,
        "Wrong body system accepted implant");
    h.assertTrue(
        inventory.insertItem(0, stack, false).isEmpty(), "Correct body system rejected implant");
    h.assertTrue(inventory.capacityUsed() == 70000, "Capacity overflow or missing item");
    h.assertTrue(
        inventory.insertItem(0, stack, false).getCount() == 1, "Socket accepted a second implant");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void equipmentNbtRoundTrip(GameTestHelper h) {
    var source = new CyberwareInventory();
    source.setStackInSlot(0, ReviewItems.cortex());
    var restored = new CyberwareInventory();
    restored.deserializeNBT(
        h.getLevel().registryAccess(), source.serializeNBT(h.getLevel().registryAccess()));
    h.assertTrue(
        restored.getSlots() == 21
            && restored.getStackInSlot(0).is(ReviewItems.cortex().getItem())
            && restored.capacityUsed() == 70000,
        "Equipment did not persist");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void playerAttachmentPersists(GameTestHelper h) {
    var p = player(h);
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(0, ReviewItems.cortex());
    CompoundTag tag = p.saveWithoutId(new CompoundTag());
    var copy = player(h);
    copy.load(tag);
    h.assertTrue(
        copy.getData(Cybernetics2077.CYBERWARE).capacityUsed() == 70000,
        "Player attachment lost equipment on save/load");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void menuSyncAndReadOnlyGuards(GameTestHelper h) {
    var p = player(h);
    p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(0, ReviewItems.cortex());
    var server = CyberwareMenu.server(1, p.getInventory());
    var client = new CyberwareMenu(1, p.getInventory());
    server.addSlotListener(
        new ContainerListener() {
          public void slotChanged(AbstractContainerMenu menu, int index, ItemStack stack) {
            client.getSlot(index).set(stack.copy());
          }

          public void dataChanged(AbstractContainerMenu menu, int index, int value) {
            client.setData(index, (short) value);
          }
        });
    server.broadcastChanges();
    h.assertTrue(
        client.capacityUsed() == 70000
            && client.capacityLimit() == 100
            && client.installedCount() == 1,
        "Native slot/data synchronization lost equipment or 32-bit stats");
    h.assertTrue(
        !server.getSlot(0).mayPickup(p) && !server.getSlot(0).mayPlace(ReviewItems.cortex()),
        "Equipment viewer allowed editing");
    server.clicked(0, 0, ClickType.PICKUP, p);
    server.clicked(0, 0, ClickType.QUICK_MOVE, p);
    server.clicked(0, 0, ClickType.THROW, p);
    h.assertTrue(
        server.getSlot(0).hasItem()
            && server.getCarried().isEmpty()
            && server.quickMoveStack(p, 0).isEmpty(),
        "Forged menu clicks changed equipment");
    p.setHealth(0);
    h.assertTrue(!server.stillValid(p), "Dead player retained equipment menu");
    h.succeed();
  }
}
