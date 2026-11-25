package dev.cybernetics2077.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Preserve primary-hit memory around our separately typed, cooldown-limited secondary damage. */
@Mixin(LivingEntity.class)
public interface LivingDamageMemory {
  @Accessor("lastHurt")
  float cybernetics$lastHurt();

  @Accessor("lastHurt")
  void cybernetics$lastHurt(float amount);
}
