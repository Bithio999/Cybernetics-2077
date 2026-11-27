package dev.cybernetics2077;

import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Searchable mod inventory, grouped by body system and ascending tier within each implant family.
 */
public final class CreativeContent {
  private static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Cybernetics2077.ID);
  public static final java.util.function.Supplier<CreativeModeTab> TAB =
      TABS.register(
          "cybernetics2077",
          () ->
              CreativeModeTab.builder()
                  .title(Component.translatable("itemGroup.cybernetics2077"))
                  .icon(() -> new ItemStack(Cybernetics2077.OPTICS.get()))
                  .hideTitle()
                  .withSearchBar()
                  .displayItems((parameters, output) -> items().forEach(output::accept))
                  .build());

  public static void register(IEventBus bus) {
    TABS.register(bus);
  }

  public static List<Item> items() {
    var items = new LinkedHashSet<Item>();
    Comparator<CyberwareCatalogue.Offer> order =
        Comparator.comparing((CyberwareCatalogue.Offer o) -> family(o.item()))
            .thenComparingInt(CyberwareCatalogue.Offer::tier);
    for (var category : CyberwareCategory.values())
      CyberwareCatalogue.offers().stream()
          .filter(o -> o.item() instanceof CyberwareItem && o.category() == category)
          .sorted(order)
          .map(CyberwareCatalogue.Offer::item)
          .forEach(items::add);
    for (var kind : QuickhackKind.values())
      for (var entry : Cybernetics2077.QUICKHACKS.get(kind)) items.add(entry.get());
    for (var entry : Cybernetics2077.DAMAGED_PARTS) items.add(entry.get());
    for (var entry : Cybernetics2077.ICONIC_REQUISITIONS) items.add(entry.get());
    items.add(Cybernetics2077.BENCH_ITEM.get());
    CityContent.BLOCK_ITEMS.values().forEach(i -> items.add(i.get()));
    items.add(CityContent.EDDIES.get());
    items.add(CityContent.CONTRACT.get());
    items.add(CityContent.BOUNTY_CONTRACT.get());
    items.add(CityContent.DOC_EGG.get());
    items.add(CityContent.FIXER_EGG.get());
    items.add(CityContent.PSYCHO_EGG.get());
    return List.copyOf(items);
  }

  private static String family(Item item) {
    if (item instanceof AugmentItem a) return a.augment().id;
    if (item instanceof OpticsItem) return "kiroshi_optics";
    if (item instanceof CyberdeckItem d)
      return d.model() == CyberdeckItem.Model.RAVEN ? "raven_microcyber" : "militech_paraline";
    if (item instanceof MantisBladesItem m) return m.variant().id + "_mantis_blades";
    return BuiltInRegistries.ITEM.getKey(item).getPath();
  }

  private CreativeContent() {}
}
