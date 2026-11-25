package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.cybernetics2077.temporal.TemporalEnvironment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public abstract class TemporalBlockAnimationMixin {
  @WrapOperation(
      method = "doAnimateTick",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
  private void cybernetics$block(
      Block block,
      BlockState state,
      Level level,
      BlockPos pos,
      RandomSource random,
      Operation<Void> original) {
    if (TemporalEnvironment.randomStep(level, pos, random))
      original.call(block, state, level, pos, random);
  }

  @WrapOperation(
      method = "doAnimateTick",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/material/FluidState;animateTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
  private void cybernetics$fluid(
      FluidState state, Level level, BlockPos pos, RandomSource random, Operation<Void> original) {
    if (TemporalEnvironment.randomStep(level, pos, random))
      original.call(state, level, pos, random);
  }
}
