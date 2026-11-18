package dev.cybernetics2077.mob;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class SalvageOddsTest {
  @Test
  void distributionIsNormalizedAndNeverLosesADrop() {
    for (double average = 1; average <= 5; average += .25)
      for (int count = 1; count <= 6; count++) {
        double sum = 0;
        for (double p : SalvageOdds.probabilities(average, count)) {
          assertTrue(p >= 0);
          sum += p;
        }
        assertEquals(1, sum, 1e-12);
        for (double r : new double[] {0, .1, .5, .99, Math.nextDown(1d)})
          assertTrue(
              SalvageOdds.roll(average, count, r) >= 1 && SalvageOdds.roll(average, count, r) <= 5);
      }
  }

  @Test
  void everyHigherTierThresholdImprovesWithBothInputs() {
    for (int threshold = 2; threshold <= 5; threshold++) {
      for (int count = 1; count <= 6; count++) {
        double previous = 0;
        for (double average = 1; average <= 5; average += .1) {
          double chance = atLeast(average, count, threshold);
          assertTrue(chance >= previous - 1e-12);
          previous = chance;
        }
      }
      for (double average = 1; average <= 5; average += .1) {
        double previous = 0;
        for (int count = 1; count <= 6; count++) {
          double chance = atLeast(average, count, threshold);
          assertTrue(chance >= previous - 1e-12);
          previous = chance;
        }
      }
    }
  }

  private double atLeast(double avg, int count, int threshold) {
    double sum = 0;
    var p = SalvageOdds.probabilities(avg, count);
    for (int t = threshold; t <= 5; t++) sum += p[t - 1];
    return sum;
  }

  @Test
  void badLoadoutsAreRejectedRatherThanMintingSalvage() {
    for (double avg : new double[] {Double.NaN, Double.POSITIVE_INFINITY, 0, 5.1})
      assertThrows(IllegalArgumentException.class, () -> SalvageOdds.roll(avg, 2, .5));
    assertThrows(IllegalArgumentException.class, () -> SalvageOdds.roll(3, 0, .5));
    assertThrows(IllegalArgumentException.class, () -> SalvageOdds.roll(3, 7, .5));
    assertThrows(IllegalArgumentException.class, () -> SalvageOdds.roll(3, 2, 1));
  }
}
