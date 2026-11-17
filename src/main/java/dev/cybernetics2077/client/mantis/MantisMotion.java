package dev.cybernetics2077.client.mantis;

import dev.cybernetics2077.combat.CombatRules;

/**
 * Continuous articulated motion: anticipation, fast contact stroke, follow-through, eased recovery.
 */
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
      float trail) {}

  public static Pose sample(
      boolean deployed,
      float sinceChange,
      float sinceAttack,
      int combo,
      boolean charging,
      float chargeAge,
      boolean left) {
    float d = smooth(Math.clamp(sinceChange / CombatRules.DEPLOY, 0, 1));
    if (!deployed) d = 1 - d;
    float panels = smooth(Math.clamp((d - .05f) / .48f, 0, 1));
    float blade = smooth(Math.clamp((d - .27f) / .68f, 0, 1));
    float side = left ? -1 : 1;
    float duration = combo == 3 ? 22 : 10;
    float t = sinceAttack / duration;
    float x = 0, y = 0, z = 0, pitch = 0, yaw = 0, roll = 0, trail = 0;
    boolean strikes = combo == 3 || left == (combo == 1);
    if (strikes && t >= 0 && t < 1) {
      float wind = smooth(Math.clamp(t / .23f, 0, 1));
      float cut = smooth(Math.clamp((t - .23f) / .25f, 0, 1));
      float recover = smooth(Math.clamp((t - .53f) / .47f, 0, 1));
      float anticipation = wind * (1 - cut), follow = cut * (1 - recover);
      if (combo == 3) {
        x = side * (-.12f * anticipation - .18f * follow);
        y = .20f * anticipation - .06f * follow;
        z = .12f * anticipation - .42f * follow;
        pitch = 48 * anticipation - 32 * follow;
        yaw = side * (22 * anticipation - 16 * follow);
        roll = side * (30 * anticipation - 24 * follow);
      } else {
        float reverse = combo == 2 ? -1 : 1;
        x = side * (.20f * anticipation - .48f * follow);
        y = -.10f * anticipation + .14f * follow;
        z = .10f * anticipation - .22f * follow;
        pitch = -18 * anticipation + 15 * follow;
        yaw = side * (30 * anticipation - 45 * follow);
        roll = side * reverse * (40 * anticipation - 64 * follow);
      }
      trail = (float) Math.sin(Math.PI * Math.clamp((t - .20f) / .35f, 0, 1));
    }
    if (charging) {
      float c = smooth(Math.clamp(chargeAge / 15, 0, 1));
      y += .18f * c;
      pitch += 40 * c;
      roll += side * 26 * c;
      z += .08f * c;
    }
    y -= .07f * (1 - d);
    z += .18f * (1 - d);
    return new Pose(panels, blade, x, y, z, pitch, yaw, roll, trail);
  }

  public static float smooth(float t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private MantisMotion() {}
}
