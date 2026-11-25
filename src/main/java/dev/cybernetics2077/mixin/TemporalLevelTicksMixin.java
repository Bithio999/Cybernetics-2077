package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cybernetics2077.temporal.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.function.BiConsumer;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelTicks.class)
public abstract class TemporalLevelTicksMixin<T> implements TemporalTickAccess {
  @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;

  @Shadow
  public abstract boolean hasScheduledTick(BlockPos pos, T type);

  @Shadow
  public abstract void schedule(ScheduledTick<T> tick);

  @Unique private ServerLevel cybernetics$owner;
  @Unique private boolean cybernetics$fluid, cybernetics$retry;

  public void cybernetics$bind(ServerLevel level, boolean fluid) {
    cybernetics$owner = level;
    cybernetics$fluid = fluid;
  }

  public ServerLevel cybernetics$level() {
    return cybernetics$owner;
  }

  public boolean cybernetics$fluid() {
    return cybernetics$fluid;
  }

  @WrapMethod(method = "addContainer")
  private void cybernetics$loaded(
      net.minecraft.world.level.ChunkPos pos, LevelChunkTicks<T> ticks, Operation<Void> original) {
    original.call(pos, ticks);
    if (cybernetics$owner != null)
      TemporalScheduled.loaded(cybernetics$owner, cybernetics$fluid, pos);
  }

  @WrapMethod(method = "removeContainer")
  private void cybernetics$unloaded(
      net.minecraft.world.level.ChunkPos pos, Operation<Void> original) {
    if (cybernetics$owner != null)
      TemporalScheduled.unloaded(cybernetics$owner, cybernetics$fluid, pos);
    original.call(pos);
  }

  @WrapMethod(method = "schedule")
  private void cybernetics$queued(ScheduledTick<T> tick, Operation<Void> original) {
    boolean exists = hasScheduledTick(tick.pos(), tick.type());
    original.call(tick);
    if (cybernetics$owner != null
        && !cybernetics$retry
        && !exists
        && hasScheduledTick(tick.pos(), tick.type()))
      TemporalScheduled.queued(cybernetics$owner, cybernetics$fluid, tick, false);
  }

  @WrapMethod(method = "tick")
  private void cybernetics$prepare(
      long now, int limit, BiConsumer<BlockPos, T> consumer, Operation<Void> original) {
    if (cybernetics$owner != null)
      TemporalScheduled.prepare(cybernetics$owner, cybernetics$fluid, allContainers);
    original.call(now, limit, consumer);
  }

  @WrapOperation(
      method = "runCollectedTicks",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Ljava/util/function/BiConsumer;accept(Ljava/lang/Object;Ljava/lang/Object;)V"))
  private void cybernetics$run(
      BiConsumer<BlockPos, T> consumer,
      Object pos,
      Object type,
      Operation<Void> original,
      @Local ScheduledTick<T> tick) {
    if (cybernetics$owner == null
        || TemporalScheduled.ready(cybernetics$owner, cybernetics$fluid, tick)) {
      original.call(consumer, pos, type);
      return;
    }
    cybernetics$retry = true;
    try {
      schedule(
          new ScheduledTick<>(
              tick.type(),
              tick.pos(),
              cybernetics$owner.getGameTime()
                  + TemporalScheduled.retryDelay(cybernetics$owner, cybernetics$fluid, tick),
              tick.priority(),
              tick.subTickOrder()));
    } finally {
      cybernetics$retry = false;
    }
  }

  @WrapMethod(method = "clearArea")
  private void cybernetics$clear(BoundingBox area, Operation<Void> original) {
    original.call(area);
    if (cybernetics$owner != null)
      TemporalScheduled.clear(cybernetics$owner, cybernetics$fluid, area);
  }

  @WrapMethod(method = "copyAreaFrom")
  @SuppressWarnings("unchecked")
  private void cybernetics$copy(
      LevelTicks<T> source, BoundingBox area, Vec3i offset, Operation<Void> original) {
    var access = (TemporalTickAccess) source;
    if (cybernetics$owner == null || access.cybernetics$level() == null) {
      original.call(source, area, offset);
      return;
    }
    var copies =
        TemporalScheduled.get(access.cybernetics$level())
            .copies(
                access.cybernetics$level(),
                cybernetics$fluid,
                area,
                offset,
                k -> hasScheduledTick(BlockPos.of(k.pos()), (T) TemporalScheduled.type(k)));
    original.call(source, area, offset);
    TemporalScheduled.get(cybernetics$owner)
        .applyCopies(
            cybernetics$owner,
            copies,
            k -> hasScheduledTick(BlockPos.of(k.pos()), (T) TemporalScheduled.type(k)));
  }
}
