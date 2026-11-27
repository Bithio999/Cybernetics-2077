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
    RAVEN,
    ARASAKA,
    BIOTECH,
    NETWATCH,
    TETRATRONIC
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
    if (model == null || tier == null || tier.level < firstTier(model))
      throw new IllegalArgumentException("Unavailable cyberdeck model/tier");
    return switch (model) {
      case RAVEN -> 10 + tier.level * 2;
      case ARASAKA -> 14;
      case BIOTECH -> 16;
      case NETWATCH -> 25;
      case TETRATRONIC -> 16;
      default -> tier.deckCapacity();
    };
  }

  public static int firstTier(Model m) {
    return m == Model.NETWATCH ? 5 : m == Model.RAVEN ? 3 : m == Model.BIOTECH ? 2 : 1;
  }

  public static String id(Model m) {
    return switch (m) {
      case PARALINE -> "militech_paraline";
      case RAVEN -> "raven_microcyber";
      case ARASAKA -> "arasaka";
      case BIOTECH -> "biotech_sigma";
      case NETWATCH -> "netwatch_netdriver";
      case TETRATRONIC -> "tetratronic_rippler";
    };
  }

  @Override
  public boolean iconic() {
    return model == Model.NETWATCH;
  }

  public int cost(QuickhackItem program) {
    return Math.max(
        1, program.cost() - (model == Model.ARASAKA && program.kind().control() ? 2 : 0));
  }

  public float durationMultiplier(QuickhackItem program) {
    return model == Model.BIOTECH && !program.kind().control()
        ? 1.6f
        : model == Model.ARASAKA && program.kind().control()
            ? 1.25f
            : model == Model.NETWATCH ? 1.5f : 1;
  }

  public float damageMultiplier(QuickhackItem program) {
    return 1
        + damageBonus()
        + (model == Model.BIOTECH
                && (program.kind() == QuickhackKind.OVERHEAT
                    || program.kind() == QuickhackKind.CONTAGION)
            ? .1f
            : 0);
  }

  public float queuedMultiplier(QuickhackItem program, QuickhackItem next) {
    return model == Model.TETRATRONIC
            && !program.kind().control()
            && next != null
            && next.kind().control()
        ? 1.4f
        : 1;
  }

  public String specialty() {
    return switch (model) {
      case ARASAKA -> "Control: -2 RAM cost, +25% duration.";
      case BIOTECH -> "Combat duration +60%; Overheat/Contagion damage +10%.";
      case NETWATCH -> "Quickhack duration +50%.";
      case TETRATRONIC ->
          "Combat then Control on same target: +40% hack damage. Controlled targets take +15%"
              + " weapon damage.";
      default -> "";
    };
  }

  public Model model() {
    return model;
  }

  public int ram() {
    return switch (model) {
      case RAVEN -> tier().level + 4;
      case ARASAKA -> tier().level + 4;
      case BIOTECH -> tier().level + 4;
      case NETWATCH -> 13;
      case TETRATRONIC -> tier().level == 5 ? 20 : tier().ram() + 2;
      default -> tier().ram();
    };
  }

  public float ramPerSecond() {
    return tier().ramPerSecond();
  }

  public float damageBonus() {
    return model == Model.NETWATCH
        ? .3f
        : model == Model.RAVEN || model == Model.ARASAKA || model == Model.BIOTECH
            ? 0
            : tier().quickhackBonus();
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
