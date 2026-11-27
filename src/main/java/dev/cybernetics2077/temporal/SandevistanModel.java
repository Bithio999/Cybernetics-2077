package dev.cybernetics2077.temporal;

/** Model strength is independent of item tier. Old saves default to the Dynalar battery. */
public enum SandevistanModel {
  DYNALAR,
  APOGEE,
  DAVID,
  ZETATECH,
  QIANT,
  FALCON;

  public boolean iconic() {
    return this == APOGEE || this == DAVID || this == FALCON;
  }

  public double slowing(int tier) {
    return switch (this) {
      case DAVID -> .95;
      case ZETATECH -> .3;
      case QIANT -> .2;
      case FALCON -> .7;
      case APOGEE -> .85;
      default -> TemporalRules.slowing(tier);
    };
  }

  public int duration(int tier) {
    return switch (this) {
      case DAVID, FALCON -> 10;
      case APOGEE -> 6;
      default -> TemporalRules.duration(tier);
    };
  }

  public int recharge(int tier) {
    return switch (this) {
      case DAVID -> 40;
      case ZETATECH -> 50 - 6 * tier;
      case QIANT -> 90 - 10 * tier;
      case FALCON -> 30;
      case APOGEE -> 25;
      default -> TemporalRules.recharge(tier);
    };
  }

  public float critChance(int tier) {
    return switch (this) {
      case DAVID -> .25f;
      case APOGEE -> .20f;
      case FALCON -> .15f;
      case ZETATECH, QIANT -> 0;
      default -> TemporalRules.critChance(tier);
    };
  }

  public float critDamage(int tier) {
    return switch (this) {
      case DAVID -> .30f;
      case APOGEE -> .20f;
      case FALCON -> .10f;
      case ZETATECH, QIANT -> 0;
      default -> TemporalRules.critDamage(tier);
    };
  }

  public float headshotMultiplier() {
    return this == DAVID ? 1.25f : this == APOGEE ? 1.2f : 1;
  }

  public int killExtensionTicks() {
    return this == DAVID ? 20 : this == APOGEE || this == FALCON ? 12 : 0;
  }

  public double minimumCharge(int tier) {
    return iconic() ? 1. / (20 * duration(tier)) : .2;
  }

  public static SandevistanModel of(dev.cybernetics2077.cyberware.Augment a) {
    if (a == null) return DYNALAR;
    return switch (a) {
      case MILITECH_APOGEE -> APOGEE;
      case DAVIDS_SANDEVISTAN -> DAVID;
      case ZETATECH_SANDEVISTAN -> ZETATECH;
      case QIANT_WARP_DANCER -> QIANT;
      case MILITECH_FALCON -> FALCON;
      default -> DYNALAR;
    };
  }

  public static SandevistanModel of(boolean apogee) {
    return of(apogee, false);
  }

  public static SandevistanModel of(boolean apogee, boolean david) {
    return david ? DAVID : apogee ? APOGEE : DYNALAR;
  }
}
