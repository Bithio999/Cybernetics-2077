package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.*;
import org.joml.*;
import org.junit.jupiter.api.Test;

final class MantisViewTest {
  @Test
  void dorsalMountFacesUpWhileBladeAxisPointsTowardTheHand() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true}) {
        var pose = MantisMotion.sample(true, 100, 100, 0, false, 0, left);
        var frame = MantisView.frame(left, true, slim, 0, 0, pose).mul(MantisRig.elbow(pose));
        var dorsal = frame.transformDirection(new Vector3f(0, 0, -1)).normalize();
        var towardHand = frame.transformDirection(new Vector3f(0, 1, 0)).normalize();
        assertTrue(dorsal.y > .85, "Dorsal mount is not up: " + dorsal);
        assertTrue(towardHand.z < -.85, "Blade does not point past hand: " + towardHand);
        assertTrue(
            java.lang.Math.abs(towardHand.y) < .35,
            "Blade is held upright instead of pointing forward");
      }
  }

  @Test
  void existingHandNeverJumpsOrCollapsesDuringHandover() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true}) {
        Vector3f previous = null;
        for (float age = 0; age <= 30; age += .025f) {
          var pose = MantisMotion.sample(true, age, 100, 0, false, 0, left);
          var m = MantisView.frame(left, true, slim, 0, 0, pose).mul(MantisRig.elbow(pose));
          var wrist = m.transformPosition(new Vector3f(0, .56f, 0));
          if (previous != null)
            assertTrue(previous.distance(wrist) < .025f, "Hand changed pose discontinuously");
          assertTrue(m.determinant() > .6f, "Hand interpolation collapsed/flipped geometry");
          previous = wrist;
        }
      }
  }

  @Test
  void tipExtendsLastAndRetractsFirstIndependentlyOfTheSupport() {
    var half = MantisMotion.sample(true, 20, 100, 0, false, 0, false);
    var open = MantisMotion.sample(true, 30, 100, 0, false, 0, false);
    assertTrue(half.extension() > .6f);
    assertEquals(0, half.tip());
    assertEquals(1, open.tip());
    assertEquals(0, MantisMotion.sample(false, 10, 100, 0, false, 0, false).tip());
    assertTrue(MantisMotion.sample(false, 10, 100, 0, false, 0, false).extension() > .6f);
  }
}
