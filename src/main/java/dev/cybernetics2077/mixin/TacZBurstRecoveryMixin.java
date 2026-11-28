package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.cybernetics2077.compat.GunRecovery;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.tacz.guns.resource.pojo.data.gun.BurstData", remap = false)
public abstract class TacZBurstRecoveryMixin {
  @ModifyReturnValue(method = "getMinInterval", at = @At("RETURN"))
  private double cybernetics$interval(double value) {
    return GunRecovery.burst(value);
  }
}
