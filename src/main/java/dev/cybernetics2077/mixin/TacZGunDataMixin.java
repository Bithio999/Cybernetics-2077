package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cybernetics2077.compat.TacZCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Scale the native interval on both sides; preserve attachments, heat and network timestamps. */
@Pseudo
@Mixin(targets = "com.tacz.guns.resource.pojo.data.gun.GunData", remap = false)
public abstract class TacZGunDataMixin {
  @ModifyReturnValue(method = "getShootInterval", at = @At("RETURN"))
  private long cybernetics$cadence(long value, @Local(argsOnly = true) LivingEntity shooter) {
    return TacZCompat.interval(shooter, value);
  }
}
