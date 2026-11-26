package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.client.temporal.BlockAnimations;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public abstract class TemporalWorldAnimationMixin {
  @WrapMethod(method = "renderLevel")
  private void cybernetics$world(
      DeltaTracker delta,
      boolean outline,
      Camera camera,
      GameRenderer renderer,
      LightTexture light,
      Matrix4f view,
      Matrix4f projection,
      Operation<Void> original) {
    BlockAnimations.begin(camera.getPosition(), delta.getGameTimeDeltaPartialTick(false));
    try {
      original.call(delta, outline, camera, renderer, light, view, projection);
    } finally {
      BlockAnimations.end();
    }
  }
}
