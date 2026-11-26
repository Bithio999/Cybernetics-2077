package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cybernetics2077.client.GunCycleTime;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.tacz.guns.api.client.animation.ObjectAnimationRunner", remap = false)
public abstract class TacZAnimationTimeMixin {
  @ModifyReturnValue(method = "elapsedSinceLastUpdate", at = @At("RETURN"))
  private long cybernetics$mechanism(long elapsed) {
    return GunCycleTime.elapsed(this, elapsed);
  }
}
