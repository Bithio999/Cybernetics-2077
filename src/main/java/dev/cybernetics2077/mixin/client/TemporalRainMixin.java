package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.cybernetics2077.client.temporal.TemporalWeather;
import dev.cybernetics2077.temporal.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class TemporalRainMixin {
  @Shadow private ClientLevel level;
  @Shadow private int ticks;
  @Unique private float cybernetics$shift;

  @WrapMethod(method = "renderSnowAndRain")
  private void cybernetics$rain(
      LightTexture light, float partial, double x, double y, double z, Operation<Void> original) {
    cybernetics$shift = 0;
    if (level != null) TemporalWeather.begin(level, ticks + (double) partial);
    try {
      original.call(light, partial, x, y, z);
    } finally {
      cybernetics$shift = 0;
    }
  }

  @WrapOperation(
      method = "renderSnowAndRain",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
  private Biome.Precipitation cybernetics$column(
      Biome biome,
      BlockPos pos,
      Operation<Biome.Precipitation> original,
      @Local(argsOnly = true) float partial) {
    var rain = original.call(biome, pos);
    cybernetics$shift =
        rain == Biome.Precipitation.RAIN
            ? TemporalWeather.shift(
                level,
                pos.getX(),
                pos.getZ(),
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y,
                ticks,
                partial)
            : 0;
    return rain;
  }

  @WrapOperation(
      method = "renderSnowAndRain",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/vertex/VertexConsumer;setUv(FF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
  private VertexConsumer cybernetics$uv(
      VertexConsumer consumer, float u, float v, Operation<VertexConsumer> original) {
    return original.call(consumer, u, v + cybernetics$shift);
  }

  @WrapOperation(
      method = "tickRain",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
  private void cybernetics$splash(
      ClientLevel world,
      ParticleOptions type,
      double x,
      double y,
      double z,
      double dx,
      double dy,
      double dz,
      Operation<Void> original) {
    double pace =
        type == ParticleTypes.RAIN ? Sandevistan.environmentPace(world, x, y, z, true) : 1;
    if (pace >= 1 || world.random.nextDouble() < pace)
      original.call(world, type, x, y, z, dx, dy, dz);
  }
}
