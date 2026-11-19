package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.temporal.SandevistanLens;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SandevistanLensMixin {
  @Inject(method = "renderLevel", at = @At("RETURN"))
  private void cybernetics$temporalLens(DeltaTracker timer, CallbackInfo ci) {
    SandevistanLens.apply();
  }
}
