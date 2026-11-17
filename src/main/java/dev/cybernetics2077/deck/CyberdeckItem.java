package dev.cybernetics2077.deck;

import dev.cybernetics2077.cyberware.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** Quickhacks belong to the physical deck, including when it is removed from the bench. */
public final class CyberdeckItem extends CyberwareItem {
  public static final int SLOTS = 2, RAM = 4;

  public CyberdeckItem() {
    super(
        new Item.Properties().component(DataComponents.CONTAINER, ItemContainerContents.EMPTY),
        CyberwareCategory.OPERATING_SYSTEM,
        8,
        CyberwareTier.ONE);
  }

  public static ItemStack chip(ItemStack deck, int index) {
    var items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    deck.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    return items.get(index);
  }

  public static boolean has(ItemStack deck, Item item) {
    return deck.getItem() instanceof CyberdeckItem
        && java.util.stream.IntStream.range(0, SLOTS).anyMatch(i -> chip(deck, i).is(item));
  }
}
