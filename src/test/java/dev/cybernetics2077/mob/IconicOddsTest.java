package dev.cybernetics2077.mob;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IconicOddsTest {
  @Test
  void rareBonusScalesWithActualLoadout() {
    assertEquals(.01, IconicOdds.chance(1, 1), 1e-9);
    assertEquals(.075, IconicOdds.chance(5, 6), 1e-9);
    for (int count = 1; count <= 6; count++)
      for (double average = 1; average < 5; average += .25) {
        assertTrue(IconicOdds.chance(average, count) < IconicOdds.chance(average + .25, count));
        if (count < 6)
          assertTrue(IconicOdds.chance(average, count) < IconicOdds.chance(average, count + 1));
      }
  }

  @Test
  void InvalidLoadoutsCannotMintAuthorization() {
    for (double average : new double[] {0, 6, Double.NaN, Double.POSITIVE_INFINITY})
      assertThrows(IllegalArgumentException.class, () -> IconicOdds.chance(average, 1));
    assertThrows(IllegalArgumentException.class, () -> IconicOdds.chance(3, 0));
    assertThrows(IllegalArgumentException.class, () -> IconicOdds.chance(3, 7));
  }
}
