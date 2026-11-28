package dev.cybernetics2077.compat;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.ExpansionRuntime;
import dev.cybernetics2077.temporal.*;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Optional query scope: keep native packets, ammo, scripts and reload timestamps untouched. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class GunRecovery {
  public interface Sampler {
    boolean cybernetics$sampleRecovery();
  }

  private static final class Query {
    long shot = -1, interval = -1;
  }

  private static final ThreadLocal<Query> QUERY = new ThreadLocal<>();
  private static final ThreadLocal<Set<Sampler>> SAMPLERS =
      ThreadLocal.withInitial(() -> Collections.newSetFromMap(new WeakHashMap<>()));

  public static long query(
      Sampler owner,
      LivingEntity shooter,
      GunRecoveryClock clock,
      long now,
      int tolerance,
      java.util.function.LongSupplier nativeQuery) {
    Query previous = QUERY.get(), current = new Query();
    QUERY.set(current);
    try {
      long result = nativeQuery.getAsLong();
      // Register before the first shot too: the next client tick must observe its timestamp
      // even if the trigger is released until after an off/on cycle.
      if (current.interval >= 0) SAMPLERS.get().add(owner);
      if (current.interval < 0 || current.shot < 0) {
        clock.reset();
        return result;
      }
      return clock.remaining(
          current.shot,
          now,
          current.interval,
          Sandevistan.weaponPace(shooter) * ExpansionRuntime.gunRate(shooter),
          tolerance);
    } finally {
      if (previous == null) QUERY.remove();
      else QUERY.set(previous);
    }
  }

  public static long shot(long value) {
    var q = QUERY.get();
    if (q != null) q.shot = value;
    return value;
  }

  /** During a recovery query we need native work, not a rescaled deadline. */
  public static boolean captureInterval(long value) {
    var q = QUERY.get();
    if (q == null) return false;
    q.interval = value;
    return true;
  }

  public static double burst(double seconds) {
    captureInterval((long) (seconds * 1000f));
    return seconds;
  }

  private static final class NativeClock {
    static final Method NOW;

    static {
      try {
        NOW = Class.forName("com.tacz.guns.util.time.GunTime").getMethod("nowMillis");
      } catch (ReflectiveOperationException e) {
        throw new IllegalStateException("TaCZ firing clock unavailable", e);
      }
    }
  }

  public static long nowMillis() {
    try {
      return ((Number) NativeClock.NOW.invoke(null)).longValue();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("TaCZ firing clock failed", e);
    }
  }

  /**
   * Sample even with the trigger released, so a completed recovery cannot return on reactivation.
   */
  public static void sampleAll() {
    for (var sampler : new ArrayList<>(SAMPLERS.get()))
      if (!sampler.cybernetics$sampleRecovery()) SAMPLERS.get().remove(sampler);
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post e) {
    sampleAll();
  }

  private GunRecovery() {}
}
