package dev.cybernetics2077.deck;

import dev.cybernetics2077.cyberware.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** Quickhacks belong to the physical deck, including when it is removed from the bench. */
public final class CyberdeckItem extends CyberwareItem {
  public enum Model {
    PARALINE,
    RAVEN
  }

  private final Model model;

  public CyberdeckItem() {
    this(CyberwareTier.ONE);
  }

  public CyberdeckItem(CyberwareTier tier) {
    this(tier, Model.PARALINE);
  }

  public CyberdeckItem(CyberwareTier tier, Model model) {
    super(
        new Item.Properties().component(DataComponents.CONTAINER, ItemContainerContents.EMPTY),
        CyberwareCategory.OPERATING_SYSTEM,
        capacity(tier, model),
        tier);
    this.model = model;
  }

  private static int capacity(CyberwareTier tier, Model model) {
    if (model == null || tier == null || model == Model.RAVEN && tier.level < 3)
      throw new IllegalArgumentException("Unavailable cyberdeck model/tier");
    return model == Model.RAVEN ? 10 + tier.level * 2 : tier.deckCapacity();
  }

  public Model model() {
    return model;
  }

  public int ram() {
    return model == Model.RAVEN ? tier().level + 4 : tier().ram();
  }

  public float ramPerSecond() {
    return tier().ramPerSecond();
  }

  public float damageBonus() {
    return model == Model.RAVEN ? 0 : tier().quickhackBonus();
  }

  public int relayTargets() {
    return model == Model.RAVEN ? tier().level - 2 : 0;
  }

  public int relayRange() {
    return model == Model.RAVEN ? 2 * tier().level - 2 : 0;
  }

  public float relayDamage() {
    return .5f;
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
    return programs(deck).stream().findFirst().orElse(null);
  }

  public static java.util.List<QuickhackItem> programs(ItemStack deck) {
    var result = new java.util.ArrayList<QuickhackItem>();
    if (!(deck.getItem() instanceof CyberdeckItem item)) return java.util.List.of();
    for (int i = 0; i < slots(deck); i++)
      if (chip(deck, i).getItem() instanceof QuickhackItem program
          && program.tier().level <= item.tier().level
          && result.stream().noneMatch(p -> p.kind() == program.kind())) result.add(program);
    return java.util.List.copyOf(result);
  }
}
