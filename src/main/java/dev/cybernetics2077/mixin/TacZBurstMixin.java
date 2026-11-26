package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cybernetics2077.compat.TacZCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "com.tacz.guns.item.ModernKineticGunScriptAPI", remap = false)
public abstract class TacZBurstMixin {
  @Shadow private LivingEntity shooter;

  @WrapOperation(
      method = "shootOnce",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBurstShootInterval()J"))
  private long cybernetics$burst(@Coerce Object data, Operation<Long> original) {
    return TacZCompat.interval(shooter, original.call(data));
  }
}
