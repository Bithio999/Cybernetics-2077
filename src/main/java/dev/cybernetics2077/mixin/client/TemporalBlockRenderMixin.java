package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.temporal.TemporalEnvironment;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class TemporalBlockRenderMixin {
  @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true)
  private float cybernetics$partial(float partial, BlockEntity entity) {
    return entity.getLevel() == null || entity instanceof PistonMovingBlockEntity
        ? partial
        : TemporalEnvironment.renderPartial(entity.getLevel(), entity.getBlockPos(), partial);
  }
}
