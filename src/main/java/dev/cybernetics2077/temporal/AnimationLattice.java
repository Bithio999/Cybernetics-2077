package dev.cybernetics2077.temporal;

import java.util.*;

/** Sparse visual clocks. Native texture animation is never stopped or replaced globally. */
public final class AnimationLattice {
  public static final int CELL = 4, LIMIT = 32768, SLOTS = 131072, PROBES = 16;

  public record Node(int x, int y, int z) {}

  public static final class Clock {
    public double lag, pace = 1, at, slow;

    Clock(double now) {
      at = slow = now;
    }

    void advance(double now) {
      if (now >= at) lag += (now - at) * (1 - pace);
      else lag = 0;
      at = now;
    }

    public float weight(double now) {
      double fade = Math.clamp((100 - (now - slow)) / 40, 0, 1);
      return (float) (fade * fade * (3 - 2 * fade));
    }
  }

  public final Map<Node, Clock> clocks = new HashMap<>();

  public void touch(int x, int y, int z, double now, double pace) {
    var key = new Node(x, y, z);
    var clock = clocks.get(key);
    if (clock == null) {
      if (pace >= 1 || clocks.size() >= LIMIT) return;
      clock = new Clock(now);
      clocks.put(key, clock);
    }
    clock.advance(now);
    clock.pace = Math.clamp(pace, .05, 1);
    if (pace < 1) clock.slow = now;
  }

  public void finish(double now) {
    clocks.values().removeIf(c -> now < c.at || now - c.slow > 100);
    for (var clock : clocks.values())
      if (clock.at != now) {
        clock.advance(now);
        clock.pace = 1;
      }
  }

  public static int hash(int x, int y, int z) {
    return (x * 73856093 ^ y * 19349663 ^ z * 83492791) & (SLOTS - 1);
  }
}
