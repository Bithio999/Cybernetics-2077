package dev.cybernetics2077.port;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

public final class PortItems {
  public static CompoundTag custom(ItemStack s) {
    return s.getTagElement("CyberneticsContract") == null
        ? new CompoundTag()
        : s.getTagElement("CyberneticsContract").copy();
  }

  public static void custom(ItemStack s, CompoundTag tag) {
    s.addTagElement("CyberneticsContract", tag.copy());
  }

  public static void load(ItemStack s, NonNullList<ItemStack> items) {
    var tag = s.getTagElement("CyberneticsPrograms");
    if (tag != null) ContainerHelper.loadAllItems(tag, items);
  }

  public static void save(ItemStack s, NonNullList<ItemStack> items) {
    var tag = new CompoundTag();
    ContainerHelper.saveAllItems(tag, items);
    s.addTagElement("CyberneticsPrograms", tag);
  }
}
