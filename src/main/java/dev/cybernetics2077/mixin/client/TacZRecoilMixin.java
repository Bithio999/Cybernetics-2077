package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cybernetics2077.compat.TacZCompat;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.CameraSetupEvent", remap = false)
public abstract class TacZRecoilMixin {
  @WrapOperation(
      method = "applyCameraRecoil",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/player/LocalPlayer;setXRot(F)V",
              remap = true))
  private static void cybernetics$pitch(LocalPlayer player, float angle, Operation<Void> original) {
    original.call(
        player, player.getXRot() + (angle - player.getXRot()) * (float) TacZCompat.recoil(player));
  }

  @WrapOperation(
      method = "applyCameraRecoil",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/player/LocalPlayer;setYRot(F)V",
              remap = true))
  private static void cybernetics$yaw(LocalPlayer player, float angle, Operation<Void> original) {
    original.call(
        player, player.getYRot() + (angle - player.getYRot()) * (float) TacZCompat.recoil(player));
  }
}
