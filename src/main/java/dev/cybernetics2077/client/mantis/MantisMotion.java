package dev.cybernetics2077.client.mantis;

import dev.cybernetics2077.combat.CombatRules;

/** Render-time combat choreography. XYZ offsets are camera-only; body joints stay attached. */
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
      float tip,
      float slide) {}

  // Choreography is fitted to the mod's ten-tick light attack. Contact remains at tick four.
  private static final float[] TIMES = {0, 1.6f, 4, 5.8f, CombatRules.LIGHT_INTERVAL};
  // Camera XYZ, shoulder pitch/yaw/roll, elbow, forearm twist, telescoping blade.
  private static final float[][] CUT = {
    {0, 0, 0, 0, 0, 0, 0, 0, 1},
    {.04f, -.02f, .06f, -7, 14, -18, -8, 10, .55f},
    {-.24f, .30f, .10f, -20, -18, 31, 8, -12, 1},
    {-.28f, .10f, .04f, 10, -32, 50, 5, -18, .78f},
    {0, 0, 0, 0, 0, 0, 0, 0, 1}
  };
  private static final float[][] RISING_CUT = {
    {0, 0, 0, 0, 0, 0, 0, 0, 1},
    {.05f, -.10f, .05f, 18, 14, -15, -12, -8, .42f},
    {-.13f, .09f, -.16f, -18, -30, 35, 8, 12, 1},
    {-.16f, .13f, -.06f, -22, -35, 45, 2, 18, .75f},
    {0, 0, 0, 0, 0, 0, 0, 0, 1}
  };

  private static float phase(float t, float a, float b) {
    return smooth(Math.clamp((t - a) / (b - a), 0, 1));
  }

  private static float pulse(float t, float a, float b, float c, float d) {
    return phase(t, a, b) * (1 - phase(t, c, d));
  }

  /** Cubic Hermite keeps velocity continuous through contact and follow-through. */
  private static float channel(float[][] keys, int k, float u, int c) {
    float a = keys[k][c], b = keys[k + 1][c], span = TIMES[k + 1] - TIMES[k];
    float ma = k == 0 ? 0 : (b - keys[k - 1][c]) / (TIMES[k + 1] - TIMES[k - 1]);
    float mb = k + 2 == keys.length ? 0 : (keys[k + 2][c] - a) / (TIMES[k + 2] - TIMES[k]);
    float u2 = u * u, u3 = u2 * u;
    return (2 * u3 - 3 * u2 + 1) * a
        + (u3 - 2 * u2 + u) * span * ma
        + (-2 * u3 + 3 * u2) * b
        + (u3 - u2) * span * mb;
  }

  public static Pose sample(
      boolean deployed,
      float sinceChange,
      float sinceAttack,
      int combo,
      boolean charging,
      float chargeAge,
      boolean left) {
    return sample(
        deployed, sinceChange, sinceAttack, combo, charging, chargeAge, left, 0, -1, false);
  }

  /** Flight is -1 outside a leap, otherwise the progress of its synchronized movement path. */
  public static Pose sample(
      boolean deployed,
      float sinceChange,
      float sinceAttack,
      int combo,
      boolean charging,
      float chargeAge,
      boolean left,
      float clock,
      float flight,
      boolean mainLeft) {
    float t = Math.clamp(sinceChange / CombatRules.DEPLOY, 0, 1);
    if (!deployed) t = 1 - t;
    float ready = phase(t, 0, .40f),
        panels = phase(t, .06f, .36f),
        hinge = phase(t, .16f, .48f),
        slide = phase(t, .50f, .68f),
        extension = phase(t, .70f, .92f),
        tip = phase(t, .88f, 1);
    float side = left ? -1 : 1;
    float inspect = phase(t, .12f, .38f) * (1 - phase(t, .72f, 1));
    float x = 0,
        y = 0,
        z = 0,
        pitch = -10 * inspect,
        yaw = side * 18 * inspect,
        roll = side * 12 * inspect,
        elbow = -5 * ready - 4 * inspect,
        twist = side * (-20 * ready + 4 * inspect),
        trail = 0,
        activity = 0;
    if (t > .95f) {
      if (combo != 3 && sinceAttack >= 0 && sinceAttack < CombatRules.LIGHT_INTERVAL) {
        activity = pulse(sinceAttack, 0, 1, 7, CombatRules.LIGHT_INTERVAL);
        boolean striking = left == (combo == 1 ? !mainLeft : mainLeft);
        if (striking) {
          int k = 0;
          while (k < TIMES.length - 2 && sinceAttack > TIMES[k + 1]) k++;
          float u = (sinceAttack - TIMES[k]) / (TIMES[k + 1] - TIMES[k]);
          var keys = combo == 2 ? RISING_CUT : CUT;
          x = side * channel(keys, k, u, 0);
          y = channel(keys, k, u, 1);
          z = channel(keys, k, u, 2);
          pitch += channel(keys, k, u, 3);
          yaw += side * channel(keys, k, u, 4);
          roll += side * channel(keys, k, u, 5);
          elbow += channel(keys, k, u, 6);
          twist += side * channel(keys, k, u, 7);
          tip *= Math.clamp(channel(keys, k, u, 8), 0, 1);
          trail = pulse(sinceAttack, 2, 3.5f, 4.5f, 6.5f);
        } else {
          x = side * .025f * activity;
          y = .012f * activity;
          z = .035f * activity;
          yaw += side * 6 * activity;
          elbow -= 4 * activity;
          tip *= 1 - .28f * activity;
        }
      }
      float pull = 0, drive = 0, recoil = 0;
      if (charging) {
        pull = phase(chargeAge, 2, 8);
        activity = Math.max(activity, pull);
      } else if (flight >= 0) {
        drive = phase(flight, .30f, .82f);
        pull = 1 - drive;
        activity = 1;
      } else if (combo == 3 && sinceAttack >= 0 && sinceAttack < CombatRules.HEAVY_INTERVAL) {
        pull = 1 - phase(sinceAttack, 0, CombatRules.impactTick(3));
        drive =
            phase(sinceAttack, 0, CombatRules.impactTick(3))
                * (1 - phase(sinceAttack, CombatRules.impactTick(3), CombatRules.HEAVY_INTERVAL));
        recoil = pulse(sinceAttack, 8, 12, 13, CombatRules.HEAVY_INTERVAL);
        activity = 1 - phase(sinceAttack, 18, CombatRules.HEAVY_INTERVAL);
      }
      if (pull > 0 || drive > 0 || recoil > 0) {
        x += side * (.06f * pull - .045f * drive);
        y += .04f * pull + .075f * drive;
        z += .24f * pull - .20f * drive + .055f * recoil;
        pitch += 24 * pull - 6 * drive;
        yaw += side * (5 * pull - 10 * drive);
        roll += side * (3 * pull - 8 * drive);
        elbow += 12 * pull - 3 * drive + 5 * recoil;
        twist += side * (3 * pull - 6 * drive);
        tip *= 1 - .72f * pull - .25f * recoil;
        if (!charging)
          trail = flight >= 0 ? phase(flight, .55f, .85f) : pulse(sinceAttack, 4, 6, 8, 12);
      }
    }
    float idle = phase(t, .9f, 1) * (1 - activity);
    float breath = (float) Math.sin(clock * Math.PI * 2 / 80),
        settle = (float) Math.sin(clock * Math.PI * 2 / 120),
        balance = (float) Math.sin(clock * Math.PI * 2 / 150);
    x += side * .003f * settle * idle;
    y += .006f * breath * idle;
    z += .003f * balance * idle;
    pitch += .35f * breath * idle;
    roll += side * .45f * balance * idle;
    elbow += .35f * settle * idle;
    twist += side * .3f * balance * idle;
    return new Pose(
        panels, hinge, x, y, z, pitch, yaw, roll, trail, elbow, extension, ready, twist, tip,
        slide);
  }

  public static float smooth(float t) {
    return Math.clamp(t * t * t * (t * (t * 6 - 15) + 10), 0, 1);
  }

  private MantisMotion() {}
}
