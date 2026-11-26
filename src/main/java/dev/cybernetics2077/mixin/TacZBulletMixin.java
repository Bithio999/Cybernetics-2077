package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.cybernetics2077.compat.TacZCompat;
import net.minecraft.world.entity.*;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.EntityKineticBullet", remap = false)
public abstract class TacZBulletMixin {
  @Inject(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/tacz/guns/entity/EntityKineticBullet;onBulletTick()V",
              shift = At.Shift.AFTER),
      cancellable = true)
  private void cybernetics$bank(CallbackInfo ci) {
    if (TacZCompat.completeBounce((Entity) (Object) this)) ci.cancel();
  }

  @ModifyVariable(method = "shootFromRotation", at = @At("HEAD"), argsOnly = true)
  private Vector2d cybernetics$spread(Vector2d spread, @Local(argsOnly = true) Entity shooter) {
    return shooter instanceof LivingEntity living
        ? new Vector2d(spread).mul(TacZCompat.spread(living))
        : spread;
  }
}
