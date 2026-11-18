package dev.cybernetics2077.city;

import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** The item is an authorization shard. Cargo and completion are exclusively server-owned. */
public final class DeliveryContracts {
  public static final int EDDIES = 1000, CRED = 5;
  public static final String NAME = "cybernetics2077.contract.delivery.name";

  /** 0 = success, 1 = no known clinic, 2 = inventory full, 3 = already completed. */
  public static int issue(ServerPlayer player, CityResident fixer) {
    if (!fixer.fixer()
        || !fixer.isAlive()
        || player.distanceToSqr(fixer) > 64
        || player.isSpectator()
        || !player.isAlive()) return 3;
    var account = player.getData(CityContent.ACCOUNT);
    if (account.deliveryStage() == 2) return 3;
    var saved = account.delivery();
    var destination =
        account.deliveryStage() == 1
            ? java.util.Optional.of(saved)
            : ClinicDirectory.get(player.server).nearest(player);
    if (destination.isEmpty()) return 1;
    if (player.getInventory().getFreeSlot() < 0 && !hasOwnedShard(player)) return 2;
    var record = destination.get();
    record.putInt("stage", 1);
    record.putUUID("owner", player.getUUID());
    record.putUUID("token", UUID.randomUUID());
    var shard = new ItemStack(CityContent.CONTRACT.get());
    shard.set(DataComponents.CUSTOM_DATA, CustomData.of(record));
    // Reissue invalidates and removes old copies, including the offhand.
    removeShards(player);
    if (!player.getInventory().add(shard)) return 2;
    boolean first = account.deliveryStage() == 0;
    account.delivery(record);
    player.getInventory().setChanged();
    // The fixer dossier deliberately has no inventory slots of its own.
    player.inventoryMenu.broadcastChanges();
    ScannerNetwork.sync(player);
    if (first) dialogue(player, fixer, "accept." + (fixer.style() == 0 ? "mara" : "ren"));
    else player.sendSystemMessage(Component.translatable("cybernetics2077.contract.reissued"));
    return 0;
  }

  private static boolean hasOwnedShard(ServerPlayer p) {
    // Inventory.add inserts into the 36 main slots, not armor or offhand slots.
    for (int i = 0; i < p.getInventory().items.size(); i++)
      if (owned(p.getInventory().getItem(i), p)) return true;
    return false;
  }

  private static boolean owned(ItemStack s, ServerPlayer p) {
    var data = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    return s.is(CityContent.CONTRACT.get())
        && data.hasUUID("owner")
        && data.getUUID("owner").equals(p.getUUID());
  }

  private static void removeShards(ServerPlayer p) {
    for (int i = 0; i < p.getInventory().getContainerSize(); i++)
      if (owned(p.getInventory().getItem(i), p)) p.getInventory().setItem(i, ItemStack.EMPTY);
  }

  public static boolean turnIn(ServerPlayer p, CityResident doctor, ItemStack shard) {
    if (doctor.fixer()
        || !doctor.isAlive()
        || !p.isAlive()
        || p.isSpectator()
        || p.level() != doctor.level()
        || p.distanceToSqr(doctor) > 64) return false;
    var account = p.getData(CityContent.ACCOUNT);
    var record = account.delivery();
    var presented = shard.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    if (account.deliveryStage() != 1
        || !owned(shard, p)
        || !presented.hasUUID("token")
        || !record.hasUUID("token")
        || !presented.getUUID("token").equals(record.getUUID("token"))) {
      p.displayClientMessage(Component.translatable("cybernetics2077.contract.invalid"), true);
      return false;
    }
    // Commit completion first; a copied stack or duplicate interaction cannot pay twice.
    record.putInt("stage", 2);
    record.remove("token");
    account.delivery(record);
    account.credit(EDDIES);
    account.reputation(CRED);
    shard.shrink(1);
    removeShards(p);
    p.getInventory().setChanged();
    p.inventoryMenu.broadcastChanges();
    ScannerNetwork.sync(p);
    dialogue(p, doctor, "complete");
    p.sendSystemMessage(
        Component.translatable("cybernetics2077.contract.reward", EDDIES, CRED)
            .withStyle(ChatFormatting.GOLD));
    return true;
  }

  private static void dialogue(ServerPlayer p, CityResident npc, String key) {
    p.sendSystemMessage(
        Component.empty()
            .append(npc.getName().copy().withStyle(ChatFormatting.AQUA))
            .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
            .append(
                Component.translatable("cybernetics2077.contract." + key)
                    .withStyle(ChatFormatting.WHITE)));
  }

  public static CompoundTag navigation(ServerPlayer p) {
    return p.getData(CityContent.ACCOUNT).deliveryStage() == 1
        ? p.getData(CityContent.ACCOUNT).delivery()
        : new CompoundTag();
  }

  private DeliveryContracts() {}
}
