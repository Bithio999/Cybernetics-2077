package dev.cybernetics2077.mob;

/** Small, monotonic loot distribution; all inputs come from the actual installed loadout. */
public final class SalvageOdds {
  public static double[] probabilities(double averageTier, int count) {
    if (!Double.isFinite(averageTier)
        || averageTier < 1
        || averageTier > 5
        || count < 1
        || count > 6) throw new IllegalArgumentException("Invalid enhanced loadout");
    double quality = averageTier + .12 * (count - 1);
    double[] weights = new double[5];
    double total = 0;
    for (int i = 0; i < 5; i++) {
      double distance = (i + 1 - quality) / .7;
      total += weights[i] = Math.exp(-distance * distance / 2);
    }
    for (int i = 0; i < 5; i++) weights[i] /= total;
    return weights;
  }

  public static int roll(double averageTier, int count, double random) {
    if (!(random >= 0 && random < 1)) throw new IllegalArgumentException("Invalid random roll");
    double[] odds = probabilities(averageTier, count);
    for (int i = 0; i < 4; i++) if ((random -= odds[i]) < 0) return i + 1;
    return 5;
  }

  private SalvageOdds() {}
}
