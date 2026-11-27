package dev.cybernetics2077.mob;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.world.item.ItemStack;

/**
 * Combat-only installations. Every family has an NPC implementation; no decorative RAM/gun gear.
 */
public final class CyberpsychoLoadouts {
  public record Plan(List<CyberwareItem> implants, int capacity, int tier) {
    public int rank() {
      return CyberpsychoRules.rank(capacity);
    }

    public int overcapacity() {
      return capacity - CyberpsychoRules.BASE_CAPACITY;
    }

    public CyberwareInventory inventory() {
      var result = new CyberwareInventory();
      for (var item : implants) {
        boolean inserted = false;
        for (int s = item.category().firstSlot();
            s < item.category().firstSlot() + item.category().sockets;
            s++) {
          var stack = new ItemStack(item);
          if (result.getStackInSlot(s).isEmpty() && result.isItemValid(s, stack)) {
            result.setStackInSlot(s, stack);
            inserted = true;
            break;
          }
        }
        if (!inserted) throw new IllegalStateException("Invalid cyberpsycho sockets");
      }
      return result;
    }
  }

  public static Plan forRank(int rank) {
    if (rank < 1 || rank > 6) throw new IllegalArgumentException("Invalid contract tier");
    int tier = rank <= 2 ? 3 : rank <= 4 ? 4 : 5;
    var kinds =
        new ArrayList<>(
            List.of(
                BIODYNE_BERSERK,
                DENSE_MARROW,
                BIONIC_JOINTS,
                SUBDERMAL_ARMOR,
                SHOCK_N_AWE,
                BLOOD_PUMP,
                BIOMONITOR,
                ADRENALINE_CONVERTER,
                STABBER,
                NEOFIBER,
                MICROGENERATOR,
                SELF_ICE,
                REINFORCED_TENDONS,
                HEAL_ON_KILL));
    if (rank >= 2) kinds.add(PARA_BELLUM);
    if (rank >= 3) {
      kinds.remove(BIONIC_JOINTS);
      kinds.add(EPIMORPHIC_SKELETON);
    }
    if (rank >= 4) kinds.add(PAIN_EDITOR);
    if (rank >= 5) {
      kinds.remove(HEAL_ON_KILL);
      kinds.add(SECOND_HEART);
    }
    if (rank >= 6) kinds.add(HANDLE_WRAP);
    List<CyberwareItem> items =
        kinds.stream().map(a -> (CyberwareItem) Cybernetics2077.augment(a, tier)).toList();
    var plan = new Plan(items, items.stream().mapToInt(CyberwareItem::capacityCost).sum(), tier);
    if (plan.rank() != rank || plan.inventory().capacityUsed() != plan.capacity())
      throw new IllegalStateException("Cyberpsycho capacity does not match contract tier");
    return plan;
  }

  private CyberpsychoLoadouts() {}
}
