package dev.cybernetics2077.combat;

/** Tier-one timing in game ticks, shared with animation and tested without a game runtime. */
public final class CombatRules {
  public static final int DEPLOY = 30, LIGHT_INTERVAL = 10, HEAVY_INTERVAL = 22, UPLOAD = 20;
  public static final float SHORT_CIRCUIT_DAMAGE = 8,
      RAM_COST = 2,
      RAM_MAX = 4,
      RAM_PER_TICK = .01f;

  public static int impactTick(int combo) {
    return combo == 3 ? 7 : 4;
  }

  public static float damage(int combo) {
    return combo == 3 ? 14 : combo == 2 ? 10 : 8;
  }

  public static boolean ready(long now, long last, int interval) {
    return now - last >= interval;
  }

  private CombatRules() {}
}
