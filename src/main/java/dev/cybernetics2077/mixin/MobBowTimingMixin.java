package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.cybernetics2077.cyberware.ExpansionRuntime;
import dev.cybernetics2077.mob.MobImplants;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Change native release recovery, preserving vanilla aiming, charge and arrow creation. */
@Mixin(RangedBowAttackGoal.class)
abstract class MobBowTimingMixin {
  @Shadow @Final private Mob mob;

  @ModifyExpressionValue(
      method = "tick",
      at =
          @At(
              value = "FIELD",
              target =
                  "Lnet/minecraft/world/entity/ai/goal/RangedBowAttackGoal;attackIntervalMin:I"))
  private int cybernetics$recovery(int original) {
    return MobImplants.supported(mob)
        ? Math.max(6, (int) Math.ceil(original / ExpansionRuntime.gunRate(mob)))
        : original;
  }
}
