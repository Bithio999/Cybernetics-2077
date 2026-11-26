package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.cybernetics2077.client.temporal.BlockAnimations;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public abstract class TemporalWorldAnimationMixin {
  @WrapMethod(method = "renderLevel")
  private void cybernetics$world(
      PoseStack pose,
      float partial,
      long time,
      boolean outline,
      Camera camera,
      GameRenderer renderer,
      LightTexture light,
      Matrix4f projection,
      Operation<Void> original) {
    BlockAnimations.begin(camera.getPosition(), partial);
    try {
      original.call(pose, partial, time, outline, camera, renderer, light, projection);
    } finally {
      BlockAnimations.end();
    }
  }
}
