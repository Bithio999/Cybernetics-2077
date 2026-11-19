package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.temporal.TrailStyle;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class TrailStyleTest {
  private static int r(int color) {
    return (color >> 16) & 255;
  }

  private static int g(int color) {
    return (color >> 8) & 255;
  }

  private static int b(int color) {
    return color & 255;
  }

  @Test
  void completeSpectrumRemainsVisibleBeforeHistoryEviction() {
    double oldest = (TrailStyle.WORLD_HISTORY - 1) * TrailStyle.WORLD_INTERVAL;
    assertTrue(oldest > 31 && oldest < TrailStyle.WORLD_LIFETIME);
    int orange = TrailStyle.color(4, false),
        green = TrailStyle.color(12, false),
        cyan = TrailStyle.color(17, false),
        blue = TrailStyle.color(23, false),
        pink = TrailStyle.color(32, false);
    assertTrue(r(orange) > b(orange) + 100);
    assertTrue(g(green) > r(green) + 80);
    assertTrue(g(cyan) > r(cyan) + 80 && b(cyan) > r(cyan) + 80);
    assertTrue(b(blue) > g(blue) + 60);
    assertTrue(r(pink) > g(pink) + 100 && b(pink) > g(pink) + 100);
    assertTrue((pink >>> 24) >= 130, "Tail color faded before being visible");
  }

  @Test
  void spectrumChangesSmoothlyRatherThanJumpingBetweenSwatches() {
    var seen = new HashSet<Integer>();
    for (double age = 5; age < 32; age += .05) {
      int a = TrailStyle.color(age, false), next = TrailStyle.color(age + .05, false);
      seen.add(a & 0xffffff);
      assertTrue(
          Math.abs(r(a) - r(next)) < 10
              && Math.abs(g(a) - g(next)) < 10
              && Math.abs(b(a) - b(next)) < 10);
    }
    assertTrue(seen.size() > 300);
  }

  @Test
  void opacityHasSmoothEntryAndExitWithoutHidingTheOlderColors() {
    assertEquals(0, TrailStyle.color(0, false) >>> 24);
    assertEquals(0, TrailStyle.color(TrailStyle.WORLD_LIFETIME, false) >>> 24);
    assertEquals(TrailStyle.color(10, false) >>> 24, TrailStyle.color(31, false) >>> 24);
    assertTrue((TrailStyle.color(39, false) >>> 24) < (TrailStyle.color(35, false) >>> 24));
  }

  @Test
  void lodSamplesBothEndsOfTheTrailAndHonorsItsBudget() {
    for (int budget = 2; budget < 12; budget++) {
      int n = 0;
      for (int i = 0; i < 12; i++) if (TrailStyle.selected(i, 12, budget)) n++;
      assertEquals(budget, n);
      assertTrue(TrailStyle.selected(0, 12, budget));
      assertTrue(TrailStyle.selected(11, 12, budget));
    }
  }

  @Test
  void firstPersonNeverDrawsTheLocalFullBody() {
    assertFalse(TrailStyle.bodyVisible(true, true));
    assertTrue(TrailStyle.bodyVisible(true, false));
    assertTrue(TrailStyle.bodyVisible(false, true));
  }

  @Test
  void firstPersonEchoesRemainSubtleAndFadeOut() {
    for (int i = 0; i < 12; i++) assertTrue((TrailStyle.color(i, true) >>> 24) <= 58);
    assertEquals(0, TrailStyle.color(12, true) >>> 24);
    assertTrue((TrailStyle.color(9, true) & 0xffffff) != (TrailStyle.color(3, true) & 0xffffff));
  }
}
