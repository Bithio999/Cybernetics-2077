package dev.cybernetics2077.combat;

import dev.cybernetics2077.mixin.LivingDamageMemory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Extra typed damage respects armor/effects and retains the interrupted weapon's hurt memory. */
public final class SecondaryDamage {
  public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
    int ticks = target.invulnerableTime;
    float last = ((LivingDamageMemory) target).cybernetics$lastHurt();
    try {
      return target.hurt(source, amount);
    } finally {
      target.invulnerableTime = Math.max(ticks, target.invulnerableTime);
      ((LivingDamageMemory) target).cybernetics$lastHurt(last);
    }
  }

  private SecondaryDamage() {}
}
