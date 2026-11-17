package dev.cybernetics2077.client.mantis;

import dev.cybernetics2077.combat.CombatRules;

/**
 * Connected joint choreography from the reference: alternating cuts and a two-arm strong thrust.
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
      float trail,
      float elbow) {}

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
    float duration = combo == 3 ? 22 : 10, t = sinceAttack / duration;
    float pitch = 0, yaw = 0, roll = 0, trail = 0, elbow = -65 * blade;
    if ((combo == 3 || left == (combo == 1)) && t >= 0 && t < 1) {
      float wind = smooth(Math.clamp(t / .23f, 0, 1));
      float cut = smooth(Math.clamp((t - .23f) / .25f, 0, 1));
      float recover = smooth(Math.clamp((t - .53f) / .47f, 0, 1));
      float anticipation = wind * (1 - cut), follow = cut * (1 - recover);
      if (combo == 3) {
        pitch = 8 * anticipation - 15 * follow;
        yaw = side * (14 * anticipation - 8 * follow);
        roll = side * (8 * anticipation - 10 * follow);
        elbow += -28 * anticipation + 18 * follow;
      } else {
        float reverse = combo == 2 ? -1 : 1;
        pitch = -6 * anticipation + reverse * 12 * follow;
        yaw = side * (20 * anticipation - 60 * follow);
        roll = side * reverse * (18 * anticipation - 60 * follow);
        elbow += -22 * anticipation + 16 * follow;
      }
      trail = (float) Math.sin(Math.PI * Math.clamp((t - .20f) / .35f, 0, 1));
    }
    if (charging) {
      float c = smooth(Math.clamp(chargeAge / 15, 0, 1));
      pitch += 6 * c;
      yaw += side * 8 * c;
      roll += side * 7 * c;
      elbow -= 15 * c;
    }
    // Animation never translates a shoulder away from its socket. Reach comes from the joints.
    return new Pose(panels, blade, 0, 0, 0, pitch, yaw, roll, trail, elbow);
  }

  public static float smooth(float t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private MantisMotion() {}
}
