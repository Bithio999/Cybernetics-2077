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
  void savedPoseKeepsItsBirthHueWhileNewerPosesChangeColor() {
    var cyan = TrailStyle.capture(100, 12);
    var blue = TrailStyle.capture(112, 24);
    var violet = TrailStyle.capture(118, 30);
    assertNotEquals(cyan.rgb(), blue.rgb());
    assertNotEquals(blue.rgb(), violet.rgb());
    for (double now = 100; now <= 143; now += .25) {
      assertEquals(cyan.rgb(), cyan.color(now, false) & 0xffffff);
      assertEquals(blue.rgb(), blue.color(now, false) & 0xffffff);
      assertEquals(violet.rgb(), violet.color(now, false) & 0xffffff);
    }
    assertTrue(g(cyan.rgb()) > r(cyan.rgb()) + 80 && b(cyan.rgb()) > r(cyan.rgb()) + 80);
    assertTrue(b(blue.rgb()) > g(blue.rgb()) + 60);
  }

  @Test
  void birthPaletteBlendsSmoothlyIncludingItsCycleBoundary() {
    var seen = new HashSet<Integer>();
    for (double phase = 0; phase < TrailStyle.COLOR_CYCLE * 2; phase += .05) {
      int a = TrailStyle.capture(1000, phase).rgb();
      int next = TrailStyle.capture(1000, phase + .05).rgb();
      seen.add(a);
      assertTrue(
          Math.abs(r(a) - r(next)) < 10
              && Math.abs(g(a) - g(next)) < 10
              && Math.abs(b(a) - b(next)) < 10);
    }
    assertTrue(seen.size() > 300);
    assertEquals(
        TrailStyle.capture(0, 0).rgb(), TrailStyle.capture(1000, TrailStyle.COLOR_CYCLE).rgb());
  }

  @Test
  void opacityFadesWithoutRotatingSavedRgb() {
    var tint = TrailStyle.capture(100, 24);
    assertEquals(0, tint.color(100, false) >>> 24);
    assertEquals(0, tint.color(100 + TrailStyle.WORLD_LIFETIME, false) >>> 24);
    assertEquals(tint.color(110, false) >>> 24, tint.color(131, false) >>> 24);
    assertTrue((tint.color(139, false) >>> 24) < (tint.color(135, false) >>> 24));
    assertEquals(tint.color(135, false) & 0xffffff, tint.color(139, false) & 0xffffff);
  }

  @Test
  void activationPhaseIsIndependentOfWorldAgeAndFirstPersonLifetime() {
    var first = TrailStyle.capture(9_000_000, 0);
    var other = TrailStyle.capture(10, 0);
    assertEquals(first.rgb(), other.rgb());
    assertTrue(g(first.rgb()) > r(first.rgb()) + 100);
    assertEquals(first.color(9_000_006, false) & 0xffffff, first.color(9_000_006, true) & 0xffffff);
    assertEquals(0, first.age(8_999_999));
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
  void firstPersonEchoesKeepTheirTintAndFadeOut() {
    var tint = TrailStyle.capture(50, 18);
    for (double now = 50; now <= 62; now += .25) {
      assertTrue((tint.color(now, true) >>> 24) <= 58);
      assertEquals(tint.rgb(), tint.color(now, true) & 0xffffff);
    }
    assertEquals(0, tint.color(62, true) >>> 24);
  }
}
