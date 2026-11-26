package dev.cybernetics2077.temporal;

/** Model strength is independent of item tier. Old saves default to the Dynalar battery. */
public enum SandevistanModel {
  DYNALAR,
  APOGEE,
  DAVID;

  public boolean iconic() {
    return this != DYNALAR;
  }

  public double slowing(int tier) {
    return switch (this) {
      case DAVID -> .95;
      case APOGEE -> .85;
      default -> TemporalRules.slowing(tier);
    };
  }

  public int duration(int tier) {
    return switch (this) {
      case DAVID -> 10;
      case APOGEE -> 6;
      default -> TemporalRules.duration(tier);
    };
  }

  public int recharge(int tier) {
    return switch (this) {
      case DAVID -> 40;
      case APOGEE -> 25;
      default -> TemporalRules.recharge(tier);
    };
  }

  public float critChance(int tier) {
    return switch (this) {
      case DAVID -> .25f;
      case APOGEE -> .20f;
      default -> TemporalRules.critChance(tier);
    };
  }

  public float critDamage(int tier) {
    return switch (this) {
      case DAVID -> .30f;
      case APOGEE -> .20f;
      default -> TemporalRules.critDamage(tier);
    };
  }

  public float headshotMultiplier() {
    return this == DAVID ? 1.25f : this == APOGEE ? 1.2f : 1;
  }

  public int killExtensionTicks() {
    return this == DAVID ? 20 : this == APOGEE ? 12 : 0;
  }

  public double minimumCharge(int tier) {
    return iconic() ? 1. / (20 * duration(tier)) : .2;
  }

  public static SandevistanModel of(boolean apogee) {
    return of(apogee, false);
  }

  public static SandevistanModel of(boolean apogee, boolean david) {
    return david ? DAVID : apogee ? APOGEE : DYNALAR;
  }
}
