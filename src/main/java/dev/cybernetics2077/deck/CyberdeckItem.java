package dev.cybernetics2077.deck;

import dev.cybernetics2077.cyberware.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** Quickhacks belong to the physical deck, including when it is removed from the bench. */
public final class CyberdeckItem extends CyberwareItem {
  public CyberdeckItem() {
    this(CyberwareTier.ONE);
  }

  public CyberdeckItem(CyberwareTier tier) {
    super(
        new Item.Properties().component(DataComponents.CONTAINER, ItemContainerContents.EMPTY),
        CyberwareCategory.OPERATING_SYSTEM,
        tier.deckCapacity(),
        tier);
  }

  public static int slots(ItemStack deck) {
    return deck.getItem() instanceof CyberdeckItem item ? item.tier().programSlots() : 0;
  }

  public static ItemStack chip(ItemStack deck, int index) {
    if (index < 0 || index >= slots(deck)) return ItemStack.EMPTY;
    var items = NonNullList.withSize(slots(deck), ItemStack.EMPTY);
    deck.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    return items.get(index);
  }

  public static boolean has(ItemStack deck, Item item) {
    return deck.getItem() instanceof CyberdeckItem
        && java.util.stream.IntStream.range(0, slots(deck)).anyMatch(i -> chip(deck, i).is(item));
  }

  /** Ignore malformed/imported programs that exceed the installed deck's tier. */
  public static QuickhackItem quickhack(ItemStack deck) {
    if (!(deck.getItem() instanceof CyberdeckItem item)) return null;
    for (int i = 0; i < slots(deck); i++)
      if (chip(deck, i).getItem() instanceof QuickhackItem program
          && program.tier().level <= item.tier().level) return program;
    return null;
  }
}
