package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.cybernetics2077.temporal.TemporalEnvironment;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class TemporalBlockEntityMixin {
  @Shadow @Final private BlockEntity blockEntity;

  @WrapMethod(method = "tick")
  private void cybernetics$localTime(Operation<Void> original) {
    var level = blockEntity.getLevel();
    if (level == null || TemporalEnvironment.step(level, blockEntity.getBlockPos()))
      original.call();
  }
}
