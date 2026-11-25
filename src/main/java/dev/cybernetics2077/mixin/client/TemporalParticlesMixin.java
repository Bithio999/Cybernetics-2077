package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cybernetics2077.client.temporal.TemporalWeather;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ParticleEngine.class)
public abstract class TemporalParticlesMixin {
  @Shadow protected ClientLevel level;

  @WrapMethod(method = "tickParticle")
  private void cybernetics$tick(Particle particle, Operation<Void> original) {
    TemporalWeather.tickParticle(level, particle, () -> original.call(particle));
  }

  @WrapOperation(
      method =
          "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/particle/Particle;render(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/Camera;F)V"))
  private void cybernetics$partial(
      Particle particle,
      VertexConsumer out,
      Camera camera,
      float partial,
      Operation<Void> original) {
    original.call(particle, out, camera, TemporalWeather.particlePartial(level, particle, partial));
  }
}
