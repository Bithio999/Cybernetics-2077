package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.ui.ScannerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Minecraft.class)
public abstract class ScannerInputMixin {
  @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
  private void cybernetics$attack(CallbackInfoReturnable<Boolean> ci) {
    if (ScannerClient.active()) ci.setReturnValue(false);
  }

  @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
  private void cybernetics$mine(boolean held, CallbackInfo ci) {
    if (ScannerClient.active()) ci.cancel();
  }

  @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
  private void cybernetics$use(CallbackInfo ci) {
    if (ScannerClient.active()) ci.cancel();
  }

  @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
  private void cybernetics$outline(Entity e, CallbackInfoReturnable<Boolean> ci) {
    if (ScannerClient.outlined(e)) ci.setReturnValue(true);
  }
}
