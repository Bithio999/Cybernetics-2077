package dev.cybernetics2077.cyberware;

import static dev.cybernetics2077.cyberware.Augment.*;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Model differences survive tier upgrades; Moore Tech does not inherit Biodyne's crit package. */
public final class BerserkSpecs {
  public static boolean active(LivingEntity e) {
    return e instanceof ServerPlayer p
        ? AugmentRuntime.berserk(p)
        : e instanceof net.minecraft.world.entity.Mob m
            && dev.cybernetics2077.mob.MobImplants.berserk(m);
  }

  public static float attackRate(LivingEntity e) {
    if (!active(e)) return 1;
    return switch (ExpansionImplants.os(e)) {
      case MOORE_TECH_BERSERK -> 1;
      case ZETATECH_BERSERK -> 1.2f;
      case MILITECH_BERSERK -> 1.3f;
      default -> 1 + (5 + AugmentRuntime.berserkTier(e) * 5) * .01f;
    };
  }

  public static float critChance(LivingEntity e) {
    return active(e) && ExpansionImplants.os(e) == BIODYNE_BERSERK
        ? (AugmentRuntime.berserkTier(e) - 1) * .05f
        : 0;
  }

  public static float critDamage(LivingEntity e) {
    return active(e) && ExpansionImplants.os(e) == BIODYNE_BERSERK
        ? 1 + (AugmentRuntime.berserkTier(e) - 1) * .25f
        : 1;
  }

  private BerserkSpecs() {}
}
