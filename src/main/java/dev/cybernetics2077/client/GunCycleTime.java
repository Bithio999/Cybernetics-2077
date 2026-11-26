package dev.cybernetics2077.client;

import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.temporal.FlightMath;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Only the gun's firing/cycling clips use weapon time; reload and handling keep their own clocks.
 */
public final class GunCycleTime {
  private static final Map<Object, String> NAMES = new WeakHashMap<>();

  public static long elapsed(Object runner, long realNanos) {
    var mc = Minecraft.getInstance();
    if (!(mc.getCameraEntity() instanceof LivingEntity living)
        || !TacZCompat.gun(living.getMainHandItem())) return realNanos;
    double pace = dev.cybernetics2077.temporal.Sandevistan.weaponPace(living);
    if (pace >= 1) return realNanos;
    String name =
        NAMES.computeIfAbsent(
            runner,
            r -> {
              try {
                Object animation = r.getClass().getMethod("getAnimation").invoke(r);
                return (String) animation.getClass().getField("name").get(animation);
              } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("TaCZ animation metadata changed", e);
              }
            });
    if (!FlightMath.mechanism(name) || TacZCompat.reloading(living)) return realNanos;
    return Math.round(realNanos * pace);
  }

  private GunCycleTime() {}
}
