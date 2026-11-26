package dev.cybernetics2077.mob;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.CyberwareCatalogue;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.CyberdeckItem;
import java.util.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Catalogue-derived families: one roll per family, never weighting a family by its tier count. */
public final class MobCyberwarePool {
  private static List<CyberwareItem> pool;
  private static List<List<CyberwareItem>> families;

  public static List<CyberwareItem> pool() {
    if (pool == null) {
      pool =
          CyberwareCatalogue.offers().stream()
              .map(CyberwareCatalogue.Offer::item)
              .filter(CyberwareItem.class::isInstance)
              .map(CyberwareItem.class::cast)
              .filter(item -> !item.iconic())
              .toList();
      var grouped = new LinkedHashMap<String, List<CyberwareItem>>();
      for (var item : pool) grouped.computeIfAbsent(family(item), k -> new ArrayList<>()).add(item);
      families = grouped.values().stream().map(List::copyOf).toList();
    }
    return pool;
  }

  private static String family(CyberwareItem item) {
    if (item instanceof AugmentItem a) return "augment:" + a.augment().id;
    if (item instanceof MantisBladesItem a) return "arms:" + a.variant();
    if (item instanceof CyberdeckItem a) return "deck:" + a.model();
    if (item instanceof OpticsItem) return "optics:kiroshi";
    return net.minecraft.core.registries.BuiltInRegistries.ITEM
        .getKey(item)
        .toString()
        .replaceFirst("_tier_[1-5]$", "");
  }

  public static List<CyberwareItem> roll(int tier, int count, RandomSource random) {
    if (tier < 1 || tier > 5 || count < 1 || count > 6)
      throw new IllegalArgumentException("Invalid mob cyberware roll");
    pool();
    var inventory = new CyberwareInventory();
    var chosen = new ArrayList<CyberwareItem>();
    var armor = Cybernetics2077.augment(Augment.SUBDERMAL_ARMOR, tier);
    inventory.setStackInSlot(armor.category().firstSlot(), new ItemStack(armor));
    chosen.add(armor);
    var candidates = new ArrayList<>(families);
    // Fisher-Yates uses the mob's own RNG; no global random state or repeated registry walks.
    for (int i = candidates.size() - 1; i > 0; i--)
      Collections.swap(candidates, i, random.nextInt(i + 1));
    for (var family : candidates) {
      if (chosen.size() >= count) break;
      if (family.getFirst() instanceof AugmentItem a && a.augment() == Augment.SUBDERMAL_ARMOR)
        continue;
      var eligible = family.stream().filter(i -> i.tier().level <= tier).toList();
      if (eligible.isEmpty()) continue;
      int first = eligible.stream().mapToInt(i -> i.tier().level).min().orElseThrow();
      int target = Math.max(first, tier - random.nextInt(2));
      var item = eligible.stream().filter(i -> i.tier().level == target).findFirst().orElseThrow();
      for (int slot = item.category().firstSlot();
          slot < item.category().firstSlot() + item.category().sockets;
          slot++) {
        if (!inventory.getStackInSlot(slot).isEmpty()) continue;
        inventory.setStackInSlot(slot, new ItemStack(item));
        chosen.add(item);
        break;
      }
    }
    return List.copyOf(chosen);
  }

  private MobCyberwarePool() {}
}
