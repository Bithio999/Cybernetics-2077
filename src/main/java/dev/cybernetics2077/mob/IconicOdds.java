package dev.cybernetics2077.mob;

/** Independent rare bonus drop: stronger and more heavily chromed mobs are better sources. */
public final class IconicOdds {
  public static double chance(double averageTier, int count) {
    if (!Double.isFinite(averageTier)
        || averageTier < 1
        || averageTier > 5
        || count < 1
        || count > 6) throw new IllegalArgumentException("Invalid enhanced loadout");
    return .01 * averageTier + .005 * (count - 1);
  }

  private IconicOdds() {}
}
