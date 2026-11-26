package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.*;
import org.joml.*;
import org.junit.jupiter.api.Test;

final class MantisViewTest {
  @Test
  void lungeMovesTheRenderedHandsBackAndThenForward() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true}) {
        var rest = MantisMotion.sample(true, 100, 100, 3, false, 0, left);
        var held = MantisMotion.sample(true, 100, 100, 3, true, 8, left);
        var strike = MantisMotion.sample(true, 100, 7, 3, false, 0, left);
        float restZ =
            MantisView.frame(left, true, slim, 0, 0, rest)
                .mul(MantisRig.elbow(rest))
                .transformPosition(new Vector3f(0, .56f, 0))
                .z;
        float heldZ =
            MantisView.frame(left, true, slim, 0, 0, held)
                .mul(MantisRig.elbow(held))
                .transformPosition(new Vector3f(0, .56f, 0))
                .z;
        float strikeZ =
            MantisView.frame(left, true, slim, 0, 0, strike)
                .mul(MantisRig.elbow(strike))
                .transformPosition(new Vector3f(0, .56f, 0))
                .z;
        assertTrue(heldZ > restZ + .12f, "The actual rendered hand must reel toward the body");
        assertTrue(strikeZ < restZ - .10f, "The actual hand must drive forward at contact");
      }
  }

  @Test
  void shoulderEndsStayOutsideTheScreenThroughoutDeploymentAndCombat() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true}) {
        for (int combo = 0; combo < 4; combo++)
          for (float age = 0; age <= 30; age += .25f) {
            checkShoulderBelowScreen(
                left, slim, MantisMotion.sample(true, 100, age, combo, false, 0, left));
          }
        for (float age = 0; age <= 30; age += .25f) {
          checkShoulderBelowScreen(
              left, slim, MantisMotion.sample(true, age, 100, 0, false, 0, left));
          checkShoulderBelowScreen(
              left, slim, MantisMotion.sample(true, 100, 100, 0, true, age, left));
        }
      }
  }

  private static void checkShoulderBelowScreen(boolean left, boolean slim, MantisMotion.Pose pose) {
    for (float aspect : new float[] {4f / 3, 16f / 9, 21f / 9}) {
      var nativeProjection =
          new Matrix4f().perspective((float) java.lang.Math.toRadians(70), aspect, .05f, 1000);
      var projection = MantisView.projection(nativeProjection, pose.ready());
      for (boolean main : new boolean[] {false, true}) {
        var upper = MantisView.frame(left, main, slim, 0, 0, pose);
        var matrix = new Matrix4f(projection).mul(upper);
        float depth = slim ? 3 / 16f : 4 / 16f, sleeve = .25f / 16;
        float X = .125f + sleeve, Z = depth / 2 + sleeve;
        var polygon = new java.util.ArrayList<Vector4f>();
        for (float[] corner : new float[][] {{-X, -Z}, {-X, Z}, {X, Z}, {X, -Z}})
          polygon.add(matrix.transform(new Vector4f(corner[0], -.125f - sleeve, corner[1], 1)));
        // Clip the entire cap polygon against the actual homogeneous viewing frustum.
        // Testing isolated projected corners misses triangles crossing the near plane.
        for (int plane = 0; plane < 6 && !polygon.isEmpty(); plane++) {
          var clipped = new java.util.ArrayList<Vector4f>();
          var previous = polygon.get(polygon.size()-1);
          float da = distance(previous, plane);
          for (var current : polygon) {
            float db = distance(current, plane);
            if ((da >= 0) != (db >= 0))
              clipped.add(new Vector4f(previous).lerp(current, da / (da - db)));
            if (db >= 0) clipped.add(current);
            previous = current;
            da = db;
          }
          polygon = clipped;
        }
        assertTrue(
            polygon.isEmpty(),
            "Exposed shoulder cap: "
                + left
                + " "
                + main
                + " "
                + aspect
                + " "
                + pose
                + " "
                + polygon);
      }
    }
  }

  private static float distance(Vector4f p, int plane) {
    return p.w + (plane % 2 == 0 ? 1 : -1) * (plane < 2 ? p.x : plane < 4 ? p.y : p.z);
  }

  @Test
  void idleHandsSitLowAndLeaveTheCrosshairClear() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true}) {
        var pose = MantisMotion.sample(true, 100, 100, 0, false, 0, left);
        var projection =
            new Matrix4f()
                .perspective(
                    (float) java.lang.Math.toRadians(MantisView.WEAPON_FOV), 16f / 9, .05f, 1000);
        var view = MantisView.frame(left, true, slim, 0, 0, pose).mul(MantisRig.elbow(pose));
        var wrist = projection.mul(view).transformProject(new Vector3f(0, .56f, 0));
        assertTrue(
            wrist.y < -.55f && wrist.y > -.85f,
            "Wrist should stay in the lower part of the view: " + wrist);
        assertTrue(java.lang.Math.abs(wrist.x) > .2f && java.lang.Math.abs(wrist.x) < .6f);
      }
  }

  @Test
  void weaponLensPreservesAspectDepthAndNativeHandAtTheStart() {
    for (float aspect : new float[] {4f / 3, 16f / 9, 21f / 9}) {
      var original =
          new Matrix4f().perspective((float) java.lang.Math.toRadians(70), aspect, .05f, 1000);
      assertEquals(original, MantisView.projection(original, 0));
      var lens = MantisView.projection(original, 1);
      assertEquals(
          MantisView.WEAPON_FOV,
          java.lang.Math.toDegrees(2 * java.lang.Math.atan(1 / lens.m11())),
          .0001);
      assertEquals(aspect, lens.m11() / lens.m00(), .00001);
      assertEquals(original.m22(), lens.m22());
      assertEquals(original.m32(), lens.m32());
      assertEquals(original.m23(), lens.m23());
    }
  }

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
        assertTrue(dorsal.y > .85, "The top of the forearm should face upward: " + dorsal);
        var third =
            new Matrix4f()
                .scale(-1, -1, 1)
                .mul(MantisRig.shoulder(left, slim, pose))
                .mul(MantisRig.elbow(pose))
                .mul(MantisRig.nativeToMount(left));
        var knuckles = third.transformDirection(new Vector3f(left ? 1 : -1, 0, 0)).normalize();
        assertTrue(
            knuckles.y > .75,
            "Native back-of-hand face must face upward in the wider third-person guard: "
                + knuckles);
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
    var half = MantisMotion.sample(true, 26, 100, 0, false, 0, false);
    var open = MantisMotion.sample(true, 30, 100, 0, false, 0, false);
    assertTrue(half.extension() > .6f);
    assertEquals(0, half.tip());
    assertEquals(1, open.tip());
    assertEquals(0, MantisMotion.sample(false, 4, 100, 0, false, 0, false).tip());
    assertTrue(MantisMotion.sample(false, 4, 100, 0, false, 0, false).extension() > .6f);
  }
}
