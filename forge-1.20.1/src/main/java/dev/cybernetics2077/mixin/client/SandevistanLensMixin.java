package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.temporal.SandevistanLens;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class SandevistanLensMixin {
  @Inject(method = "renderLevel", at = @At("RETURN"))
  private void cybernetics$lens(
      float partial, long time, com.mojang.blaze3d.vertex.PoseStack pose, CallbackInfo ci) {
    SandevistanLens.apply();
  }
}
