package dev.cybernetics2077.client.mantis;

import dev.cybernetics2077.combat.CombatRules;

/** Presentation clock starts on the first rendered frame, independent of packet age. */
public final class MantisDeployment {
  private final float from, to;
  private double start = Double.NaN;
  private float last;

  public MantisDeployment(float from, float to) {
    this.from = from;
    this.to = to;
    last = from;
  }

  public float sample(double tick) {
    if (Double.isNaN(start)) start = tick;
    double duration = CombatRules.DEPLOY * Math.abs(to - from);
    float t = duration == 0 ? 1 : (float) Math.clamp((tick - start) / duration, 0, 1);
    last = from + (to - from) * t;
    return last;
  }

  public float last() {
    return last;
  }

  public boolean visible() {
    return to > 0 || last > 0;
  }
}
