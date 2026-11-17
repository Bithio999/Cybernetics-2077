package dev.cybernetics2077.client.mantis;

import dev.cybernetics2077.combat.CombatRules;

/** Joint animation and independent hatch, folding hinge and telescoping carriage channels. */
public final class MantisMotion {
  public record Pose(
      float panels,
      float blade,
      float x,
      float y,
      float z,
      float pitch,
      float yaw,
      float roll,
      float trail,
      float elbow,
      float extension,
      float ready,
      float twist,
      float tip) {}

  private static float phase(float t, float a, float b) {
    return smooth(Math.clamp((t - a) / (b - a), 0, 1));
  }

  public static Pose sample(
      boolean deployed,
      float sinceChange,
      float sinceAttack,
      int combo,
      boolean charging,
      float chargeAge,
      boolean left) {
    float t = Math.clamp(sinceChange / CombatRules.DEPLOY, 0, 1);
    if (!deployed) t = 1 - t;
    float ready = phase(t, 0, .40f),
        panels = phase(t, .18f, .42f),
        hinge = phase(t, .36f, .64f),
        extension = phase(t, .52f, .76f);
    float side = left ? -1 : 1;
    float inspect = phase(t, .12f, .38f) * (1 - phase(t, .72f, 1));
    float pitch = -10 * inspect, yaw = side * 18 * inspect, roll = side * 12 * inspect;
    float elbow = -5 * ready - 4 * inspect, twist = side * (-20 * ready + 4 * inspect), trail = 0;
    float duration = combo == 3 ? 22 : 10, a = sinceAttack / duration;
    if ((combo == 3 || left == (combo == 1)) && a >= 0 && a < 1 && t > .95f) {
      float wind = phase(a, 0, .23f), cut = phase(a, .23f, .48f), recover = phase(a, .56f, 1);
      float anticipation = wind * (1 - cut), follow = cut * (1 - recover);
      if (combo == 3) {
        pitch += 18 * anticipation - 20 * follow;
        yaw += side * (18 * anticipation - 28 * follow);
        roll += side * (12 * anticipation - 14 * follow);
        elbow += -20 * anticipation + 5 * follow;
        twist += side * (-12 * anticipation + 18 * follow);
      } else {
        float reverse = combo == 2 ? -1 : 1;
        pitch += -15 * anticipation + reverse * 20 * follow;
        yaw += side * (28 * anticipation - 82 * follow);
        roll += side * reverse * (25 * anticipation - 72 * follow);
        elbow += -14 * anticipation + 5 * follow;
        twist += side * reverse * (12 * anticipation - 35 * follow);
      }
      extension -= .08f * follow;
      trail = (float) Math.sin(Math.PI * Math.clamp((a - .2f) / .35f, 0, 1));
    }
    if (charging) {
      float c = phase(chargeAge, 0, 12);
      pitch += 12 * c;
      yaw += side * 15 * c;
      roll += side * 8 * c;
      elbow -= 12 * c;
      twist -= side * 12 * c;
    }
    return new Pose(
        panels,
        hinge,
        0,
        0,
        0,
        pitch,
        yaw,
        roll,
        trail,
        elbow,
        extension,
        ready,
        twist,
        phase(t, .72f, .98f));
  }

  public static float smooth(float t) {
    return Math.clamp(t * t * t * (t * (t * 6 - 15) + 10), 0, 1);
  }

  private MantisMotion() {}
}
