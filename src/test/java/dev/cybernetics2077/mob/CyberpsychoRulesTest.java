package dev.cybernetics2077.mob;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CyberpsychoRulesTest {
  @Test
  void overcapacityBandsUseStrictBoundariesAndSaturateAtElite() {
    for (int r = 1; r <= 6; r++) {
      assertEquals(r, CyberpsychoRules.rank(150 + (r - 1) * 25 + 1));
      assertEquals(r, CyberpsychoRules.rank(150 + r * 25));
    }
    assertEquals(6, CyberpsychoRules.rank(Integer.MAX_VALUE));
    for (int n : new int[] {Integer.MIN_VALUE, 0, 149, 150})
      assertThrows(IllegalArgumentException.class, () -> CyberpsychoRules.rank(n));
  }

  @Test
  void proposedRealLoadoutsMatchDifficultyAndRiseMonotonically() {
    int[] costs = {162, 187, 215, 250, 270, 278};
    for (int i = 0; i < costs.length; i++) assertEquals(i + 1, CyberpsychoRules.rank(costs[i]));
  }
}
