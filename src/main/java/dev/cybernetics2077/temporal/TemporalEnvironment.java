package dev.cybernetics2077.temporal;

import dev.cybernetics2077.Cybernetics2077;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Standard block-entity tickers share a local fractional clock, on the server and client. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class TemporalEnvironment {
  private static final class Gate {
    long last = Long.MIN_VALUE;
    boolean step;
    final TemporalRules.Budget budget = new TemporalRules.Budget();
  }

  private static final class Clocks {
    final Long2ObjectOpenHashMap<Gate> gates = new Long2ObjectOpenHashMap<>();
    long swept = Long.MIN_VALUE;
  }

  private static final ThreadLocal<Map<Level, Clocks>> CLOCKS =
      ThreadLocal.withInitial(WeakHashMap::new);

  public static double pace(Level level, BlockPos pos) {
    String namespace =
        net.minecraft.core.registries.BuiltInRegistries.BLOCK
            .getKey(level.getBlockState(pos).getBlock())
            .getNamespace();
    if (Cybernetics2077.TIME_BUBBLE_EXCLUDED_BLOCK_NAMESPACES.get().contains(namespace)) return 1;
    return Sandevistan.environmentPace(
        level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, false);
  }

  public static boolean step(Level level, BlockPos pos) {
    double pace = pace(level, pos);
    var clocks = CLOCKS.get().get(level);
    if (pace >= 1) {
      if (clocks != null) clocks.gates.remove(pos.asLong());
      return true;
    }
    if (clocks == null) {
      clocks = new Clocks();
      CLOCKS.get().put(level, clocks);
    }
    long now = level.getGameTime();
    // Discard inactive machines/old chunks without retaining positions indefinitely.
    if (now - clocks.swept >= 40 || clocks.swept == Long.MIN_VALUE) {
      clocks.gates.values().removeIf(g -> now - g.last > 40);
      clocks.swept = now;
    }
    var gate =
        clocks.gates.computeIfAbsent(
            pos.asLong(),
            k -> {
              var created = new Gate();
              created.budget.phase = 1 - pace;
              return created;
            });
    if (gate.last != now) {
      gate.step = gate.budget.advance(pace) > 0;
      gate.last = now;
    }
    return gate.step;
  }

  public static float renderPartial(Level level, BlockPos pos, float partial) {
    var clocks = CLOCKS.get().get(level);
    var gate = clocks == null ? null : clocks.gates.get(pos.asLong());
    return gate == null
        ? partial
        : TemporalRules.partial(gate.budget.phase, pace(level, pos), partial);
  }

  public static boolean randomStep(
      Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
    double pace = pace(level, pos);
    return pace >= 1 || random.nextDouble() < pace;
  }

  @SubscribeEvent
  public static void cleanup(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
    var clocks = CLOCKS.get().get(event.getLevel());
    if (clocks == null) return;
    long now = event.getLevel().getGameTime();
    if (now - clocks.swept >= 40 || clocks.swept == Long.MIN_VALUE) {
      clocks.gates.values().removeIf(g -> now - g.last > 40);
      clocks.swept = now;
    }
  }

  @SubscribeEvent
  public static void unload(LevelEvent.Unload e) {
    CLOCKS.get().remove(e.getLevel());
  }

  private TemporalEnvironment() {}
}
