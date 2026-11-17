package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.*;
import org.joml.*;
import org.junit.jupiter.api.Test;

final class MantisViewTest {
  @Test
  void nativeKnuckleFaceIsUpInFirstAndThirdPersonWhileBladePointsPastTheHand() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true}) {
        var pose = MantisMotion.sample(true, 100, 100, 0, false, 0, left);
        var frame = MantisView.frame(left, true, slim, 0, 0, pose).mul(MantisRig.elbow(pose));
        var dorsal =
            new Matrix4f(frame)
                .mul(MantisRig.nativeToMount(left))
                .transformDirection(new Vector3f(left ? 1 : -1, 0, 0))
                .normalize();
        var towardHand = frame.transformDirection(new Vector3f(0, 1, 0)).normalize();
        assertTrue(dorsal.y > .85, "Dorsal mount is not up: " + dorsal);
        var third =
            new Matrix4f()
                .scale(-1, -1, 1)
                .mul(MantisRig.shoulder(left, slim, pose))
                .mul(MantisRig.elbow(pose))
                .mul(MantisRig.nativeToMount(left));
        var knuckles = third.transformDirection(new Vector3f(left ? 1 : -1, 0, 0)).normalize();
        assertTrue(knuckles.y > .85, "Native back-of-hand face must be upward in third person");
        assertTrue(towardHand.z < -.85, "Blade does not point past hand: " + towardHand);
        assertTrue(
            java.lang.Math.abs(towardHand.y) < .35,
            "Blade is held upright instead of pointing forward");
      }
  }

  @Test
  void skinBasisRotatesTheActualOuterFaceWithoutMirroringOrStretchingEitherArmWidth() {
    for (boolean left : new boolean[] {false, true})
      for (float depth : new float[] {3 / 16f, 4 / 16f}) {
        var basis = MantisRig.nativeToMount(left);
        var dorsal = basis.transformDirection(new Vector3f(left ? 1 : -1, 0, 0));
        assertTrue(dorsal.distance(new Vector3f(0, 0, -1)) < .000001f);
        assertTrue(
            java.lang.Math.abs(basis.transformDirection(new Vector3f(0, 0, -1)).z) < .000001f,
            "The native front face must not be mistaken for the knuckle face");
        assertEquals(1, basis.determinant(), .000001);
        for (float x : new float[] {-depth / 2, depth / 2})
          for (float z : new float[] {-.125f, .125f}) {
            var original = new Vector3f(x, .5f, z);
            var mounted = basis.transformPosition(new Vector3f(original));
            assertEquals(.125f, java.lang.Math.abs(mounted.x), .000001);
            assertEquals(depth / 2, java.lang.Math.abs(mounted.z), .000001);
            assertTrue(
                MantisRig.mountToNative(left).transformPosition(mounted).distance(original)
                    < .000001);
          }
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
    var half = MantisMotion.sample(true, 24, 100, 0, false, 0, false);
    var open = MantisMotion.sample(true, 30, 100, 0, false, 0, false);
    assertTrue(half.extension() > .6f);
    assertEquals(0, half.tip());
    assertEquals(1, open.tip());
    assertEquals(0, MantisMotion.sample(false, 6, 100, 0, false, 0, false).tip());
    assertTrue(MantisMotion.sample(false, 6, 100, 0, false, 0, false).extension() > .6f);
  }
}
