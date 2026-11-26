package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cybernetics2077.compat.TacZCompat;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.gameplay.LocalPlayerShoot", remap = false)
public abstract class TacZClientBurstMixin {
  @Shadow @Final private LocalPlayer player;

  @WrapOperation(
      method = "doShoot",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBurstShootInterval()J"))
  private long cybernetics$burst(@Coerce Object data, Operation<Long> original) {
    return TacZCompat.interval(player, original.call(data));
  }
}
