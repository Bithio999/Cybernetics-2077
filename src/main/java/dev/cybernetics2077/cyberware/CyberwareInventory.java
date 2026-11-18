package dev.cybernetics2077.cyberware;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class CyberwareInventory extends ItemStackHandler {
  public CyberwareInventory() {
    super(CyberwareCategory.totalSlots());
  }

  @Override
  public int getSlotLimit(int slot) {
    return 1;
  }

  @Override
  public boolean isItemValid(int slot, ItemStack stack) {
    if (stack.getItem() instanceof AugmentItem candidate)
      for (int i = 0; i < getSlots(); i++)
        if (i != slot
            && getStackInSlot(i).getItem() instanceof AugmentItem existing
            && candidate.augment() == existing.augment()) return false;
    return stack.isEmpty()
        || stack.getItem() instanceof CyberwareItem item
            && item.category() == CyberwareCategory.forSlot(slot);
  }

  public int capacityUsed() {
    long sum = 0;
    for (int i = 0; i < getSlots(); i++)
      if (getStackInSlot(i).getItem() instanceof CyberwareItem item) sum += item.capacityCost();
    return (int) Math.min(Integer.MAX_VALUE, sum);
  }
}
