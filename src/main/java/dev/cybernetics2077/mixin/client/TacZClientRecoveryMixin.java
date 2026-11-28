package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.compat.*;
import dev.cybernetics2077.temporal.GunRecoveryClock;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerShoot", remap = false)
public abstract class TacZClientRecoveryMixin implements GunRecovery.Sampler {
  @Shadow @Final private LocalPlayer player;

  @Shadow
  public abstract long getClientShootCoolDown();

  @Unique private final GunRecoveryClock cybernetics$recovery = new GunRecoveryClock();

  @WrapMethod(method = "getCoolDown")
  private long cybernetics$remaining(
      @Coerce Object gun, ItemStack stack, @Coerce Object data, Operation<Long> original) {
    return GunRecovery.query(
        this,
        player,
        cybernetics$recovery,
        GunRecovery.nowMillis(),
        0,
        () -> original.call(gun, stack, data));
  }

  @ModifyExpressionValue(
      method = "getCoolDown",
      at =
          @At(
              value = "FIELD",
              target =
                  "Lcom/tacz/guns/client/gameplay/LocalPlayerDataHolder;clientShootTimestamp:J"))
  private long cybernetics$shot(long timestamp) {
    return GunRecovery.shot(timestamp);
  }

  @Override
  public boolean cybernetics$sampleRecovery() {
    if (!player.isAlive() || !TacZCompat.gun(player.getMainHandItem())) return false;
    getClientShootCoolDown();
    return true;
  }
}
