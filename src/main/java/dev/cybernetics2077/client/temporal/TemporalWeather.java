package dev.cybernetics2077.client.temporal;

import dev.cybernetics2077.mixin.client.TemporalParticleAccess;
import dev.cybernetics2077.temporal.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.util.RandomSource;

/** Per-column rain phase stays continuous on activation, movement, overlap and deactivation. */
public final class TemporalWeather {
  private static final class Rain {
    final Long2ObjectOpenHashMap<LocalTime.Visual> columns = new Long2ObjectOpenHashMap<>();
    double swept = Double.NEGATIVE_INFINITY;
  }

  private static final Map<ClientLevel, Rain> RAIN = new WeakHashMap<>();
  private static final Map<Particle, TemporalRules.Budget> PARTICLES = new WeakHashMap<>();
  private static final RandomSource RANDOM = RandomSource.create();
  public static double lastPace = 1, lastShift;
  public static long slowedColumns, slowedParticles;

  public static void begin(ClientLevel level, double now) {
    var rain = RAIN.computeIfAbsent(level, l -> new Rain());
    if (now - rain.swept >= 20 || now < rain.swept) {
      rain.columns.values().removeIf(v -> now - v.last > 60 || now < v.last);
      rain.swept = now;
    }
  }

  public static float shift(ClientLevel level, int x, int z, double y, int ticks, float partial) {
    double now = ticks + (double) partial;
    double pace = Sandevistan.environmentPace(level, x + .5, y, z + .5, true);
    var rain = RAIN.computeIfAbsent(level, l -> new Rain());
    long key = ((long) z << 32) | (x & 0xffffffffL);
    var clock = rain.columns.get(key);
    lastPace = pace;
    if (clock == null && pace >= 1) {
      lastShift = 0;
      return 0;
    }
    if (clock == null) {
      clock = new LocalTime.Visual(now);
      rain.columns.put(key, clock);
    }
    double local = clock.advance(now, pace);
    if (pace < 1) slowedColumns++;
    int seed = x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
    RANDOM.setSeed(seed);
    float speed = 3 + RANDOM.nextFloat();
    int offset = (x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761) & 255;
    float vanilla = -((ticks & 131071) + offset + partial) / 32f * speed;
    float slowed = -(float) (local % 131072 + offset) / 32f * speed;
    float shift = slowed % 32 - vanilla % 32;
    lastShift = shift;
    return shift;
  }

  private static double pace(ClientLevel level, Particle p) {
    var pos = (TemporalParticleAccess) p;
    return Sandevistan.environmentPace(
        level, pos.cybernetics$x(), pos.cybernetics$y(), pos.cybernetics$z(), true);
  }

  public static void tickParticle(ClientLevel level, Particle p, Runnable original) {
    if (!(p instanceof WaterDropParticle)) {
      original.run();
      return;
    }
    double pace = pace(level, p);
    if (pace >= 1) {
      PARTICLES.remove(p);
      original.run();
      return;
    }
    slowedParticles++;
    var clock = PARTICLES.computeIfAbsent(p, k -> new TemporalRules.Budget());
    if (clock.advance(pace) > 0) original.run();
  }

  public static float particlePartial(ClientLevel level, Particle p, float partial) {
    var clock = PARTICLES.get(p);
    return clock == null ? partial : TemporalRules.partial(clock.phase, pace(level, p), partial);
  }

  private TemporalWeather() {}
}
