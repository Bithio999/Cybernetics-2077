package dev.cybernetics2077.city;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.*;

/** Named contract targets use combat implants and persistent, idempotent chassis upgrades. */
public final class BountyCombat {
  public static List<CyberwareItem> loadout(int tier, int count) {
    var inventory = new CyberwareInventory();
    var chosen = new ArrayList<CyberwareItem>();
    for (var a :
        new Augment[] {
          Augment.SUBDERMAL_ARMOR,
          Augment.BLOOD_PUMP,
          Augment.NEOFIBER,
          Augment.EPIMORPHIC_SKELETON,
          Augment.BIONIC_JOINTS,
          Augment.ADRENALINE_CONVERTER,
          Augment.SHOCK_N_AWE,
          Augment.SHOCK_ABSORBER,
          Augment.BIOMONITOR,
          Augment.VISUAL_CORTEX_SUPPORT
        }) {
      if (chosen.size() == count) break;
      if (a.firstTier > tier) continue;
      var item = Cybernetics2077.augment(a, tier);
      for (int slot = a.category.firstSlot();
          slot < a.category.firstSlot() + a.category.sockets;
          slot++)
        if (inventory.getStackInSlot(slot).isEmpty()) {
          inventory.setStackInSlot(slot, new net.minecraft.world.item.ItemStack(item));
          chosen.add(item);
          break;
        }
    }
    if (chosen.size() != count)
      throw new IllegalStateException("Insufficient combat implant sockets");
    return List.copyOf(chosen);
  }

  public static double damage(int rank, boolean psycho) {
    return (1.35 + .12 * rank) * (psycho ? 1.2 : 1);
  }

  private static void set(
      Mob m,
      net.minecraft.core.Holder<Attribute> attr,
      String name,
      double amount,
      AttributeModifier.Operation op) {
    var instance = m.getAttribute(attr);
    if (instance == null) return;
    var id = Cybernetics2077.id("bounty_" + name);
    instance.removeModifier(id);
    instance.addPermanentModifier(new AttributeModifier(id, amount, op));
  }

  public static void apply(Mob mob, int rank, boolean psycho, boolean fill) {
    set(
        mob,
        Attributes.MAX_HEALTH,
        "health",
        (2 + .35 * rank) * (psycho ? 1.5 : 1) - 1,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    set(
        mob,
        Attributes.ATTACK_DAMAGE,
        "damage",
        damage(rank, psycho) - 1,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    set(mob, Attributes.ARMOR, "armor", rank * 2, AttributeModifier.Operation.ADD_VALUE);
    set(
        mob,
        Attributes.ARMOR_TOUGHNESS,
        "toughness",
        rank * .5,
        AttributeModifier.Operation.ADD_VALUE);
    set(
        mob,
        Attributes.MOVEMENT_SPEED,
        "speed",
        psycho ? .2 : .08,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    set(mob, Attributes.FOLLOW_RANGE, "pursuit", 32, AttributeModifier.Operation.ADD_VALUE);
    if (fill) mob.setHealth(mob.getMaxHealth());
  }

  private BountyCombat() {}
}
