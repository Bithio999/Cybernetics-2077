package dev.cybernetics2077.city;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.QuickhackItem;
import java.util.*;
import net.minecraft.world.item.Item;

/** One canonical, server-priced catalogue shared by both clinic sizes. */
public final class CyberwareCatalogue {
  public record Offer(Item item, int tier, int price, CyberwareCategory category) {
    public boolean iconic() {
      return item instanceof CyberwareItem implant && implant.iconic();
    }

    public int requiredStreetCred() {
      return (tier - 1) * 10;
    }
  }

  private static List<Offer> offers;

  public static List<Offer> offers() {
    if (offers == null) {
      var list = new ArrayList<Offer>();
      for (var family :
          List.of(
              Cybernetics2077.OPTICS_TIERS,
              Cybernetics2077.CYBERDECK_TIERS,
              Cybernetics2077.MANTIS_TIERS,
              Cybernetics2077.SHORT_CIRCUIT_TIERS)) for (var entry : family) add(list, entry.get());
      for (var kind : Augment.values())
        if (kind.ordinal() <= Augment.FORTIFIED_ANKLES.ordinal())
          for (var entry : Cybernetics2077.AUGMENTS.get(kind)) add(list, entry.get());
      for (var entry : Cybernetics2077.RAVEN_TIERS) add(list, entry.get());
      for (var kind : dev.cybernetics2077.deck.QuickhackKind.values())
        if (kind != dev.cybernetics2077.deck.QuickhackKind.SHORT_CIRCUIT)
          for (var entry : Cybernetics2077.QUICKHACKS.get(kind)) add(list, entry.get());
      // Append new families: saved daily stock IDs keep referring to the same original offers.
      for (var kind : Augment.values())
        if (kind.ordinal() > Augment.FORTIFIED_ANKLES.ordinal() && !kind.iconic())
          for (var entry : Cybernetics2077.AUGMENTS.get(kind)) add(list, entry.get());
      for (var variant : MantisVariant.values())
        if (variant != MantisVariant.PHYSICAL)
          for (var entry : Cybernetics2077.MANTIS_VARIANTS.get(variant)) add(list, entry.get());
      // Iconics append after every existing offer, including elemental arm variants.
      for (var kind : Augment.values())
        if (kind.iconic())
          for (var entry : Cybernetics2077.AUGMENTS.get(kind)) add(list, entry.get());
      offers = List.copyOf(list);
    }
    return offers;
  }

  private static void add(List<Offer> list, Item item) {
    int t = item instanceof CyberwareItem i ? i.tier().level : ((QuickhackItem) item).tier().level;
    int base =
        item instanceof CyberwareItem i && i.iconic()
            ? 3000
            : item instanceof QuickhackItem ? 300 : item instanceof MantisBladesItem ? 1600 : 1000;
    list.add(
        new Offer(item, t, base * t * t, item instanceof CyberwareItem i ? i.category() : null));
  }

  private CyberwareCatalogue() {}
}
