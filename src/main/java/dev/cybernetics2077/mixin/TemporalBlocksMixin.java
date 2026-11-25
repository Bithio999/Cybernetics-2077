package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cybernetics2077.temporal.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class TemporalBlocksMixin {
  @Inject(method = "<init>", at = @At("TAIL"))
  private void cybernetics$bind(CallbackInfo ci) {
    var level = (ServerLevel) (Object) this;
    ((TemporalTickAccess) level.getBlockTicks()).cybernetics$bind(level, false);
    ((TemporalTickAccess) level.getFluidTicks()).cybernetics$bind(level, true);
  }

  @WrapOperation(
      method = "tickChunk",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
  private void cybernetics$randomBlock(
      BlockState state,
      ServerLevel level,
      BlockPos pos,
      RandomSource random,
      Operation<Void> original) {
    if (TemporalEnvironment.randomStep(level, pos, random))
      original.call(state, level, pos, random);
  }

  @WrapOperation(
      method = "tickChunk",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/material/FluidState;randomTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
  private void cybernetics$randomFluid(
      FluidState state,
      net.minecraft.world.level.Level level,
      BlockPos pos,
      RandomSource random,
      Operation<Void> original) {
    if (TemporalEnvironment.randomStep(level, pos, random))
      original.call(state, level, pos, random);
  }
}
