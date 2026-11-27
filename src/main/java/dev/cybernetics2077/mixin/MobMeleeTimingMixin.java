package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cybernetics2077.mob.MobImplants;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Vanilla melee goals do not consult ATTACK_SPEED. Apply equipped NPC recovery at the reset. */
@Mixin(MeleeAttackGoal.class)
abstract class MobMeleeTimingMixin {
  @Shadow @Final protected PathfinderMob mob;

  @ModifyReturnValue(method = "getAttackInterval", at = @At("RETURN"))
  private int cybernetics$interval(int original) {
    return MobImplants.supported(mob)
        ? Math.max(6, (int) Math.ceil(original / MobImplants.meleeRate(mob)))
        : original;
  }
}
