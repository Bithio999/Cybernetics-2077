package dev.cybernetics2077.port;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class PortSlot extends Slot {
  private final BooleanSupplier enabled;

  public PortSlot(Inventory inventory, int slot, int x, int y, BooleanSupplier enabled) {
    super(inventory, slot, x, y);
    this.enabled = enabled;
  }

  public boolean isActive() {
    return enabled.getAsBoolean();
  }

  public boolean mayPickup(Player player) {
    return enabled.getAsBoolean();
  }

  public boolean mayPlace(ItemStack stack) {
    return enabled.getAsBoolean();
  }
}
