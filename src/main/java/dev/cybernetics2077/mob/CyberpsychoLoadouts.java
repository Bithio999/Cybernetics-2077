package dev.cybernetics2077.mob;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import java.util.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Seeded roles and socket-valid capacity budgets; the same dossier always produces the same body.
 */
public final class CyberpsychoLoadouts {
  public enum Role {
    BERSERKER,
    NETRUNNER,
    BLITZ
  }

  public record Plan(List<CyberwareItem> implants, int capacity, int tier, long seed, Role role) {
    public int rank() {
      return CyberpsychoRules.rank(capacity);
    }

    public int overcapacity() {
      return capacity - 150;
    }

    public CyberwareInventory inventory() {
      var inv = new CyberwareInventory();
      for (var item : implants)
        if (!insert(inv, stack(item, seed)))
          throw new IllegalStateException("Invalid psycho sockets");
      return inv;
    }
  }

  public static boolean insert(CyberwareInventory inv, ItemStack stack) {
    if (!(stack.getItem() instanceof CyberwareItem item)) return false;
    for (int s = item.category().firstSlot();
        s < item.category().firstSlot() + item.category().sockets;
        s++)
      if (inv.getStackInSlot(s).isEmpty() && inv.isItemValid(s, stack)) {
        inv.setStackInSlot(s, stack);
        return true;
      }
    return false;
  }

  public static ItemStack stack(CyberwareItem item, long seed) {
    var stack = new ItemStack(item);
    if (item instanceof CyberdeckItem deck) {
      var rng = new SplittableRandom(seed);
      var pool =
          new ArrayList<>(
              Arrays.stream(QuickhackKind.values())
                  .filter(k -> k.firstTier <= item.tier().level)
                  .toList());
      for (int i = pool.size() - 1; i > 0; i--) Collections.swap(pool, i, rng.nextInt(i + 1));
      int amount =
          Math.min(
              deck.tier().programSlots(), Math.min(pool.size(), Math.max(1, item.tier().level)));
      var chips = NonNullList.withSize(deck.tier().programSlots(), ItemStack.EMPTY);
      // Always include an offensive program; a low-tier deck must not roll only controls.
      pool.remove(QuickhackKind.SHORT_CIRCUIT);
      pool.addFirst(QuickhackKind.SHORT_CIRCUIT);
      for (int i = 0; i < amount; i++) {
        var kind = pool.get(i);
        int t = Math.max(kind.firstTier, item.tier().level - rng.nextInt(2));
        chips.set(i, new ItemStack(Cybernetics2077.quickhack(kind, t)));
      }
      stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(chips));
    }
    return stack;
  }

  public static Plan forRank(int rank) {
    return forSeed(rank, rank * 674953L);
  }

  public static Plan forSeed(int rank, long seed) {
    if (rank < 1 || rank > 6) throw new IllegalArgumentException("Invalid contract tier");
    var rng = new SplittableRandom(seed);
    Role role = Role.values()[rng.nextInt(3)];
    int tier = rank <= 2 ? 3 : rank <= 4 ? 4 : 5,
        low = 151 + (rank - 1) * 25,
        high = rank == 6 ? 325 : 150 + rank * 25;
    int wanted = rng.nextInt(low, high + 1);
    for (int attempt = 0; attempt < 300; attempt++) {
      var inv = new CyberwareInventory();
      var chosen = new ArrayList<CyberwareItem>();
      CyberwareItem os;
      if (role == Role.NETRUNNER) {
        var models =
            Arrays.stream(CyberdeckItem.Model.values())
                .filter(
                    m -> m != CyberdeckItem.Model.NETWATCH && CyberdeckItem.firstTier(m) <= tier)
                .toList();
        os = Cybernetics2077.deck(models.get(rng.nextInt(models.size())), tier);
      } else {
        Augment[] models =
            role == Role.BLITZ
                ? new Augment[] {DYNALAR_SANDEVISTAN, ZETATECH_SANDEVISTAN, QIANT_WARP_DANCER}
                : new Augment[] {BIODYNE_BERSERK, MOORE_TECH_BERSERK, ZETATECH_BERSERK};
        os = Cybernetics2077.augment(models[rng.nextInt(models.length)], tier);
      }
      chosen.add(os);
      insert(inv, new ItemStack(os));
      var candidates =
          new ArrayList<>(
              Arrays.stream(Augment.values())
                  .filter(
                      a ->
                          !a.iconic()
                              && !a.creativeOnly()
                              && a.firstTier <= tier
                              && a.capacity > 0
                              && a.category != CyberwareCategory.OPERATING_SYSTEM
                              && usable(a, role))
                  .toList());
      for (int i = candidates.size() - 1; i > 0; i--)
        Collections.swap(candidates, i, rng.nextInt(i + 1));
      if (role != Role.NETRUNNER && rng.nextBoolean()) {
        var variants = MantisVariant.values();
        var arms =
            Cybernetics2077.MANTIS_VARIANTS
                .get(variants[rng.nextInt(variants.length)])
                .get(tier - 2)
                .get();
        if (inv.capacityUsed() + arms.capacityCost() <= high && insert(inv, new ItemStack(arms)))
          chosen.add(arms);
      }
      // Reliable baseline protection, while the remainder is different for each dossier.
      var plating = Cybernetics2077.augment(SUBDERMAL_ARMOR, Math.max(1, tier - rng.nextInt(2)));
      insert(inv, new ItemStack(plating));
      chosen.add(plating);
      for (var kind : candidates) {
        if (kind == SUBDERMAL_ARMOR || inv.capacityUsed() + kind.capacity > high) continue;
        int t = Math.max(kind.firstTier, tier - rng.nextInt(2));
        var item = Cybernetics2077.augment(kind, t);
        if (insert(inv, new ItemStack(item))) chosen.add(item);
        if (inv.capacityUsed() >= wanted) break;
      }
      if (inv.capacityUsed() >= low && inv.capacityUsed() <= high) {
        var plan = new Plan(List.copyOf(chosen), inv.capacityUsed(), tier, seed, role);
        if (plan.rank() != rank) throw new IllegalStateException("Incorrect capacity band");
        return plan;
      }
    }
    throw new IllegalStateException(
        "Could not allocate cyberpsycho capacity budget: " + rank + "/" + seed);
  }

  private static boolean usable(Augment a, Role role) {
    boolean ram =
        Set.of(
                EX_DISK,
                MEMORY_BOOST,
                RAM_UPGRADE,
                FEEN_X,
                RAM_RECOUP,
                CAMILLO_RAM_MANAGER,
                BIOCONDUCTOR,
                COGITO_LATTICE,
                MECHATRONIC_CORE)
            .contains(a);
    if (ram && role != Role.NETRUNNER) return false;
    if (Set.of(ATOMIC_SENSORS, TITANIUM_BONES, BEHAVIORAL_FACEPLATE).contains(a)) return false;
    if (a.category == CyberwareCategory.HANDS && role != Role.NETRUNNER)
      return a == MICROGENERATOR || a == HANDLE_WRAP;
    return true;
  }

  private CyberpsychoLoadouts() {}
}
