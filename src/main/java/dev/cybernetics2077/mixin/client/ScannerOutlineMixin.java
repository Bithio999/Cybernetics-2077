package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.ui.ScannerClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ScannerOutlineMixin {
  @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
  private void cybernetics$color(CallbackInfoReturnable<Integer> ci) {
    if (ScannerClient.outlined((Entity) (Object) this)) ci.setReturnValue(0xff333e);
  }
}
