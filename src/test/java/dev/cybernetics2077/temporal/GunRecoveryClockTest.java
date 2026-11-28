package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class GunRecoveryClockTest {
  @Test
  void normalRecoveryCannotReturnWhenExtremeSlowIsReactivated() {
    var c = new GunRecoveryClock();
    assertEquals(80000, c.remaining(1000, 1000, 100, .00125, 0));
    assertEquals(79900, c.remaining(1000, 1100, 100, .00125, 0));
    assertEquals(100, c.remaining(1000, 1100, 100, 1, 0));
    assertEquals(0, c.remaining(1000, 1200, 100, 1, 0));
    assertEquals(0, c.remaining(1000, 1300, 100, .00125, 0));
    assertEquals(80000, c.remaining(1300, 1300, 100, .00125, 0));
  }

  @Test
  void partiallyRecoveredGunPreservesWorkRatherThanRescalingAllElapsedTime() {
    var c = new GunRecoveryClock();
    assertEquals(100, c.remaining(0, 0, 100, 1, 0));
    assertEquals(50, c.remaining(0, 50, 100, 1, 0));
    assertEquals(40000, c.remaining(0, 50, 100, .00125, 0));
    assertEquals(39900, c.remaining(0, 150, 100, .00125, 0));
    assertEquals(50, c.remaining(0, 150, 100, 1, 0));
    assertEquals(0, c.remaining(0, 200, 100, 1, 0));
  }

  @Test
  void rejectedOlderNetworkQueryDoesNotRewindOrRecountProgress() {
    var c = new GunRecoveryClock();
    c.remaining(0, 0, 100, 1, 0);
    assertEquals(50, c.remaining(0, 50, 100, 1, 0));
    assertEquals(70, c.remaining(0, 30, 100, 1, 0));
    assertEquals(50, c.remaining(0, 50, 100, 1, 0));
  }

  @Test
  void vanillaTimingToleranceAndNativeScriptOffsetsRemainUsable() {
    var c = new GunRecoveryClock();
    assertEquals(65, c.remaining(1000, 1030, 100, 1, 5));
    // A pack script adjusts the native shot timestamp by 20ms.
    assertEquals(85, c.remaining(1020, 1030, 100, 1, 5));
    assertEquals(115, c.remaining(1050, 1030, 100, 1, 5));
    c.reset();
    assertEquals(100, c.remaining(1000, 1000, 100, 1, 0));
  }
}
