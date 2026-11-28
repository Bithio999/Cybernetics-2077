package dev.cybernetics2077.temporal;

/** A single encounter clock preserves relative speeds without multiplying two opposing slows. */
public final class TemporalRules {
  public static final double RADIUS = 32;
  public static final double CARRIER = 1.25;
  public static final double MAX_SLOW = .999;
  public static final double MIN_PACE = 1 - MAX_SLOW;

  public static double acceleration(double slowing) {
    return 1 / (1 - Math.clamp(slowing, 0, MAX_SLOW));
  }

  public static double slowing(int tier) {
    return tier >= 2 && tier <= 5 ? .5 : 0;
  }

  public static int duration(int tier) {
    return 5 + tier;
  }

  public static int recharge(int tier) {
    return 55 - tier * 5;
  }

  public static float critChance(int tier) {
    return (tier * 3) * .01f;
  }

  public static float critDamage(int tier) {
    return tier * .03f;
  }

  public static double pace(double personal, double strongest) {
    if (strongest <= 1) return 1;
    return CARRIER * Math.clamp(personal, 1, strongest) / strongest;
  }

  public static float partial(double phase, double pace, float realPartial) {
    return (float) Math.clamp(phase + pace * realPartial, 0, 1);
  }

  public static final class Budget {
    public double phase;

    public int advance(double pace) {
      phase += Math.clamp(pace, TemporalRules.MIN_PACE, CARRIER);
      int steps = (int) Math.floor(phase + 1e-9);
      phase = Math.max(0, phase - steps);
      return steps;
    }
  }

  private TemporalRules() {}
}
