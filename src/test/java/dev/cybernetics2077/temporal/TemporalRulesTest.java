package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TemporalRulesTest {
  @Test
  void everyDynalarTierHasTheSameSlow() {
    for (int t = 2; t <= 5; t++) assertEquals(.5, TemporalRules.slowing(t));
  }

  @Test
  void strongerModelsCompareProportionally() {
    double strong = TemporalRules.acceleration(.8), weak = TemporalRules.acceleration(.6);
    assertEquals(2, TemporalRules.pace(strong, strong) / TemporalRules.pace(weak, strong), 1e-9);
    assertEquals(5, TemporalRules.pace(strong, strong) / TemporalRules.pace(1, strong), 1e-9);
  }

  @Test
  void equalUsersRemainEqual() {
    assertEquals(TemporalRules.pace(2, 2), TemporalRules.pace(2, 2));
    assertEquals(1, TemporalRules.pace(1, 1));
  }

  @Test
  void overlappingFieldsUseMaximumRatherThanMultiplication() {
    assertEquals(.625, TemporalRules.pace(1, Math.max(2, 2)), 1e-9);
  }

  @Test
  void fractionalClockDoesNotLoseTime() {
    for (double pace : new double[] {.001, .00125, .025, .25, .375, .625, 1, 1.25}) {
      var b = new TemporalRules.Budget();
      int n = 0;
      for (int i = 0; i < 4000; i++) n += b.advance(pace);
      assertEquals(4000 * pace, n, 1e-8);
    }
  }

  @Test
  void interpolationMovesSmoothlyBetweenVirtualTicks() {
    assertEquals(.5, TemporalRules.partial(.25, .5, .5f), 1e-6);
    assertEquals(1, TemporalRules.partial(.9, .5, 1));
  }

  @Test
  void higherTiersImproveDurationAndRechargeInstead() {
    assertTrue(TemporalRules.duration(5) > TemporalRules.duration(2));
    assertTrue(TemporalRules.recharge(5) < TemporalRules.recharge(2));
  }
}
