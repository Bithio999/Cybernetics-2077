package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class LocalTimeTest {
  @Test
  void fixedDelayConsumesLocalTimeRatherThanOnlyDeferringItsLastTick() {
    var t = new LocalTime.Timer(10, 0);
    for (int i = 1; i <= 15; i++) {
      t.advance(i, .625);
      assertTrue(t.remaining > 0);
    }
    t.advance(16, .625);
    assertEquals(0, t.remaining, 1e-9);
  }

  @Test
  void leavingAFieldResumesTheRemainingDelayWithoutReplayingMissedUpdates() {
    var t = new LocalTime.Timer(20, 0);
    t.advance(8, .625);
    assertEquals(15, t.remaining);
    t.advance(22, 1);
    assertEquals(1, t.remaining);
    t.advance(23, 1);
    assertEquals(0, t.remaining);
    t.advance(23, 1);
    assertEquals(0, t.remaining);
  }

  @Test
  void aVisualClockChangesSpeedWithoutJumpingAtActivationAndDeactivation() {
    var c = new LocalTime.Visual(100.25);
    assertEquals(100.25, c.advance(100.25, .625));
    assertEquals(106.5, c.advance(110.25, .625));
    assertEquals(106.5, c.advance(110.25, 1));
    assertEquals(116.5, c.advance(120.25, 1));
  }

  @Test
  void frameSubdivisionDoesNotChangeVisualTime() {
    var many = new LocalTime.Visual(5);
    var one = new LocalTime.Visual(5);
    for (int i = 1; i <= 600; i++) many.advance(5 + i / 60.0, .625);
    assertEquals(one.advance(15, .625), many.time, 1e-9);
  }

  @Test
  void remainingDelayCanBeRestoredExactlyAfterRestart() {
    var old = new LocalTime.Timer(100, 500);
    old.advance(532, .625);
    var restored = new LocalTime.Timer(old.remaining, old.last);
    restored.advance(532, .625);
    assertEquals(80, restored.remaining);
    restored.advance(612, 1);
    assertEquals(0, restored.remaining);
  }
}
