package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.combat.LungePath;
import org.junit.jupiter.api.Test;

final class LungePathTest {
  @Test
  void groundAndAirLeapsReachTheSameTargetInsteadOfLosingTravelToFriction() {
    for (double height : new double[] {0, 1.25, 2.5}) {
      var path = new LungePath(0, height, 0, 0, 0, 6.5, .65, 9);
      double x = 0, y = height, z = 0, maxY = y;
      for (int age = 0; age < path.ticks(); age++) {
        var d = path.step(x, y, z, age);
        assertTrue(Math.sqrt(d.x() * d.x() + d.y() * d.y() + d.z() * d.z()) <= 1.35001);
        x += d.x();
        y += d.y();
        z += d.z();
        maxY = Math.max(maxY, y);
      }
      assertEquals(6.5, z, .00001);
      assertEquals(0, y, .00001);
      if (height == 0) assertTrue(maxY > .6, "Ground leap has no rise");
    }
  }

  @Test
  void latePacketsCannotCreateAnUnboundedTeleport() {
    var p = new LungePath(0, 0, 0, 0, 0, 8, .65, 10);
    var delta = p.step(0, 0, 0, 9);
    assertEquals(
        1.35,
        Math.sqrt(delta.x() * delta.x() + delta.y() * delta.y() + delta.z() * delta.z()),
        .00001);
    assertEquals(p.sample(10), p.sample(999));
  }
}
