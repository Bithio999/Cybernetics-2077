package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.client.temporal.AnimationShaders;
import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class TemporalBlockShadersMixin {
  @Inject(method = "getRendertypeSolidShader", at = @At("RETURN"), cancellable = true)
  private static void cybernetics$solid(CallbackInfoReturnable<ShaderInstance> ci) {
    ci.setReturnValue(AnimationShaders.select("solid", ci.getReturnValue()));
  }

  @Inject(method = "getRendertypeCutoutShader", at = @At("RETURN"), cancellable = true)
  private static void cybernetics$cutout(CallbackInfoReturnable<ShaderInstance> ci) {
    ci.setReturnValue(AnimationShaders.select("cutout", ci.getReturnValue()));
  }

  @Inject(method = "getRendertypeCutoutMippedShader", at = @At("RETURN"), cancellable = true)
  private static void cybernetics$mipped(CallbackInfoReturnable<ShaderInstance> ci) {
    ci.setReturnValue(AnimationShaders.select("cutout_mipped", ci.getReturnValue()));
  }

  @Inject(method = "getRendertypeTranslucentShader", at = @At("RETURN"), cancellable = true)
  private static void cybernetics$translucent(CallbackInfoReturnable<ShaderInstance> ci) {
    ci.setReturnValue(AnimationShaders.select("translucent", ci.getReturnValue()));
  }

  @Inject(
      method = "getRendertypeTranslucentMovingBlockShader",
      at = @At("RETURN"),
      cancellable = true)
  private static void cybernetics$moving(CallbackInfoReturnable<ShaderInstance> ci) {
    ci.setReturnValue(AnimationShaders.select("translucent_moving_block", ci.getReturnValue()));
  }
}
