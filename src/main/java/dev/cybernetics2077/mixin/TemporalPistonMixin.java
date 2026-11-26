package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cybernetics2077.temporal.PistonTime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(PistonMovingBlockEntity.class)
public abstract class TemporalPistonMixin {
  @ModifyConstant(method = "tick", constant = @Constant(floatValue = .5f))
  private static float cybernetics$distance(
      float amount, Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity piston) {
    return amount * (float) PistonTime.pace(level, pos, piston);
  }

  @WrapOperation(
      method = "moveCollidedEntities",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
  private static void cybernetics$slime(
      Entity entity,
      double x,
      double y,
      double z,
      Operation<Void> original,
      @Local(argsOnly = true) Level level,
      @Local(argsOnly = true) BlockPos pos,
      @Local(argsOnly = true) PistonMovingBlockEntity piston) {
    double pace = PistonTime.pace(level, pos, piston);
    var axis = piston.getMovementDirection().getAxis();
    original.call(
        entity,
        axis == net.minecraft.core.Direction.Axis.X ? x * pace : x,
        axis == net.minecraft.core.Direction.Axis.Y ? y * pace : y,
        axis == net.minecraft.core.Direction.Axis.Z ? z * pace : z);
  }
}
