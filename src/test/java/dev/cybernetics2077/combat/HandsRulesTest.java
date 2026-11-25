package dev.cybernetics2077.combat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class HandsRulesTest {
  @Test
  void reflectionFlipsOnlyTheSurfaceNormalComponent() {
    var v = HandsRules.reflect(2, -3, 4, 0, 1, 0);
    assertEquals(2, v.x());
    assertEquals(3, v.y());
    assertEquals(4, v.z());
    var back = HandsRules.reflect(v.x(), v.y(), v.z(), 0, 1, 0);
    assertEquals(-3, back.y());
  }

  @Test
  void guidanceHasABoundedTurnAndDoesNotIncreaseSpeed() {
    for (double cap : new double[] {.02, .05, .1}) {
      var v = HandsRules.turn(0, 0, 3, 1, 0, 1, cap);
      assertEquals(3, Math.sqrt(v.x() * v.x() + v.y() * v.y() + v.z() * v.z()), 1e-9);
      assertTrue(Math.acos(v.z() / 3) <= cap + 1e-9);
      assertTrue(v.x() > 0);
    }
  }

  @Test
  void guidanceCannotReverseTowardABehindTarget() {
    var v = HandsRules.turn(0, 0, 3, 0, 0, -1, .1);
    assertEquals(3, v.z());
    assertEquals(0, v.x());
  }

  @Test
  void guidanceConvergesWithoutOvershootingAndHandlesDegenerateVectors() {
    var v = HandsRules.turn(0, 0, 2, .001, 0, 1, .1);
    assertEquals(Math.atan(.001), Math.atan2(v.x(), v.z()), 1e-8);
    assertEquals(new HandsRules.Vector(0, 0, 0), HandsRules.turn(0, 0, 0, 1, 1, 1, .1));
    assertEquals(new HandsRules.Vector(0, 0, 2), HandsRules.turn(0, 0, 2, 0, 0, 0, .1));
  }

  @Test
  void everyTierKeepsBouncesSlowerAndHasABoundedChargingRate() {
    for (int t = 1; t <= 5; t++) {
      assertTrue(HandsRules.bounceSpeed(t) < 1 && HandsRules.bounceSpeed(t) > .6);
      assertTrue(HandsRules.chargePeriod(t) >= 3);
    }
    assertTrue(HandsRules.bounceSpeed(5) > HandsRules.bounceSpeed(1));
  }
}
