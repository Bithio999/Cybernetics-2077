package dev.cybernetics2077.cyberware;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CarryReserveMenu extends AbstractContainerMenu {
  private final DataSlot unlocked = DataSlot.standalone();

  public CarryReserveMenu(int id, Inventory inventory) {
    super(Cybernetics2077.RESERVE_MENU.get(), id);
    unlocked.set(9 * ((AugmentRuntime.tier(inventory.player, Augment.TITANIUM_BONES) + 1) / 2));
    addDataSlot(unlocked);
    var reserve = inventory.player.getData(Cybernetics2077.CARRY_RESERVE);
    for (int i = 0; i < 27; i++) {
      final int index = i;
      addSlot(
          new SlotItemHandler(reserve, i, 8 + i % 9 * 18, 18 + i / 9 * 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return index < unlocked.get();
            }

            @Override
            public boolean isActive() {
              return index < unlocked.get() || hasItem();
            }
          });
    }
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
    for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 143));
  }

  @Override
  public boolean stillValid(Player p) {
    return p.isAlive() && AugmentRuntime.tier(p, Augment.TITANIUM_BONES) > 0;
  }

  @Override
  public ItemStack quickMoveStack(Player p, int index) {
    var slot = slots.get(index);
    if (!slot.hasItem()) return ItemStack.EMPTY;
    var stack = slot.getItem();
    var copy = stack.copy();
    if (index < 27) {
      if (!moveItemStackTo(stack, 27, 63, true)) return ItemStack.EMPTY;
    } else if (!moveItemStackTo(stack, 0, Math.min(27, unlocked.get()), false))
      return ItemStack.EMPTY;
    if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    return copy;
  }
}
