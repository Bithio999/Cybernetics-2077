package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.compat.*;
import dev.cybernetics2077.temporal.GunRecoveryClock;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.shooter.LivingEntityShoot", remap = false)
public abstract class TacZRecoveryMixin implements GunRecovery.Sampler {
  @Shadow @Final private LivingEntity shooter;

  @Shadow
  public abstract long getShootCoolDown();

  @Unique private final GunRecoveryClock cybernetics$recovery = new GunRecoveryClock();

  @WrapMethod(method = "getShootCoolDown(J)J")
  private long cybernetics$remaining(long timestamp, Operation<Long> original) {
    return GunRecovery.query(
        this, shooter, cybernetics$recovery, timestamp, 5, () -> original.call(timestamp));
  }

  @ModifyExpressionValue(
      method = "getShootCoolDown(J)J",
      at =
          @At(
              value = "FIELD",
              target = "Lcom/tacz/guns/entity/shooter/ShooterDataHolder;shootTimestamp:J"))
  private long cybernetics$shot(long timestamp) {
    return GunRecovery.shot(timestamp);
  }

  @Override
  public boolean cybernetics$sampleRecovery() {
    if (!shooter.isAlive() || !TacZCompat.gun(shooter.getMainHandItem())) return false;
    getShootCoolDown();
    return true;
  }
}
