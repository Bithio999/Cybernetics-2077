package dev.cybernetics2077.temporal;

/** Model strength is independent of item tier. Old saves default to the Dynalar battery. */
public enum SandevistanModel {
  DYNALAR,
  APOGEE;

  public double slowing(int tier) {
    return this == APOGEE ? .85 : TemporalRules.slowing(tier);
  }

  public int duration(int tier) {
    return this == APOGEE ? 6 : TemporalRules.duration(tier);
  }

  public int recharge(int tier) {
    return this == APOGEE ? 25 : TemporalRules.recharge(tier);
  }

  public float critChance(int tier) {
    return this == APOGEE ? .20f : TemporalRules.critChance(tier);
  }

  public float critDamage(int tier) {
    return this == APOGEE ? .20f : TemporalRules.critDamage(tier);
  }

  public double minimumCharge(int tier) {
    return this == APOGEE ? 1. / (20 * duration(tier)) : .2;
  }

  public static SandevistanModel of(boolean apogee) {
    return apogee ? APOGEE : DYNALAR;
  }
}
