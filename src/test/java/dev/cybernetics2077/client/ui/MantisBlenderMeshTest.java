package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.*;
import java.io.*;
import java.nio.file.*;
import org.joml.*;
import org.junit.jupiter.api.Test;

final class MantisBlenderMeshTest {
  private static MantisMeshData read(String lod) throws IOException {
    try (var in =
        Files.newInputStream(
            Path.of(
                "src/main/resources/assets/cybernetics2077/meshes/mantis/" + lod + ".cymesh"))) {
      return MantisMeshData.read(in);
    }
  }

  @Test
  void foldedReceiverRisesWithTheSupportThenSlidesBackBeforeUnfolding() {
    for (float rise = 0; rise <= 1; rise += .025f) {
      var upper = MantisArticulation.blade(rise, 0);
      var folded = upper.transformDirection(new Vector3f(0, 1, 0));
      assertEquals(-1, folded.y, .00001, "Relative fold must stay locked during the shared lift");
      assertEquals(0, folded.z, .00001);
    }
    var fixed = MantisArticulation.mount(1);
    var before = new Matrix4f(fixed).mul(MantisArticulation.carriage(0));
    var after = new Matrix4f(fixed).mul(MantisArticulation.carriage(1));
    assertEquals(-.06f, after.m31() - before.m31(), .00001);
    assertEquals(before.m30(), after.m30());
    assertEquals(before.m32(), after.m32());
    var raised = MantisMotion.sample(true, 15, 100, 0, false, 0, false);
    var slid = MantisMotion.sample(true, 21, 100, 0, false, 0, false);
    assertEquals(1, raised.blade());
    assertEquals(0, raised.extension());
    assertEquals(1, slid.slide());
    assertEquals(0, slid.extension());
  }

  @Test
  void optimizedMeshesFitBudgetsAndHaveValidUVsAndNormals() throws Exception {
    var near = read("near");
    var far = read("far");
    int n = near.parts().values().stream().mapToInt(MantisMeshData.Part::triangles).sum();
    int f = far.parts().values().stream().mapToInt(MantisMeshData.Part::triangles).sum();
    assertTrue(n < 3800 && n > 1000);
    assertTrue(f < 700 && f < n * .20);
    for (var model : new MantisMeshData[] {near, far})
      for (var part : model.parts().values()) {
        var v = part.vertices();
        assertEquals(part.triangles() * 3 * 8, v.length);
        for (int i = 0; i < v.length; i += 8) {
          assertTrue(v[i + 3] >= 0 && v[i + 3] <= 1 && v[i + 4] >= 0 && v[i + 4] <= 1);
          double normal = v[i + 5] * v[i + 5] + v[i + 6] * v[i + 6] + v[i + 7] * v[i + 7];
          assertEquals(1, normal, .0001);
        }
      }
  }

  @Test
  void distantModelKeepsMajorSilhouetteAndPartOrigins() throws Exception {
    var near = read("near");
    var far = read("far");
    for (var name : near.parts().keySet()) {
      float[] a = near.parts().get(name).vertices(), b = far.parts().get(name).vertices();
      for (int axis = 0; axis < 3; axis++) {
        float minA = Float.POSITIVE_INFINITY, maxA = -minA, minB = minA, maxB = -minA;
        for (int i = axis; i < a.length; i += 8) {
          minA = java.lang.Math.min(minA, a[i]);
          maxA = java.lang.Math.max(maxA, a[i]);
        }
        for (int i = axis; i < b.length; i += 8) {
          minB = java.lang.Math.min(minB, b[i]);
          maxB = java.lang.Math.max(maxB, b[i]);
        }
        assertEquals(minA, minB, .01, "LOD changed part alignment: " + name);
        assertEquals(maxA, maxB, .01, "LOD changed part silhouette: " + name);
      }
    }
  }

  @Test
  void corruptOrTruncatedMeshIsRejected() throws Exception {
    assertThrows(
        IOException.class, () -> MantisMeshData.read(new ByteArrayInputStream(new byte[12])));
    byte[] data =
        Files.readAllBytes(
            Path.of("src/main/resources/assets/cybernetics2077/meshes/mantis/near.cymesh"));
    assertThrows(
        IOException.class,
        () ->
            MantisMeshData.read(
                new ByteArrayInputStream(java.util.Arrays.copyOf(data, data.length - 9))));
  }

  @Test
  void mechanismStaysWithinTheOpenForearmFootprintUntilItClearsTheSkin() throws Exception {
    for (String lod : new String[] {"near", "far"}) {
      var model = read(lod);
      for (float depth : new float[] {3 / 16f, 4 / 16f})
        for (float age = 0; age <= 30; age += .125f) {
          var motion = MantisMotion.sample(true, age, 100, 0, false, 0, false);
          var matrix = MantisArticulation.mount(motion.blade(), depth);
          for (String name :
              new String[] {"mount", "carriage", "support", "blade", "tip", "edge"}) {
            if (name.equals("carriage")) matrix.mul(MantisArticulation.carriage(motion.slide()));
            if (name.equals("support"))
              matrix.mul(MantisArticulation.support(motion.blade(), motion.extension()));
            if (name.equals("blade"))
              matrix.mul(MantisArticulation.blade(motion.blade(), motion.extension()));
            if (name.equals("tip")) matrix.mul(MantisArticulation.tip(motion.tip()));
            if (name.equals("edge")) matrix.mul(MantisArticulation.edge(motion.tip()));
            var v = model.parts().get(name).vertices();
            for (int i = 0; i < v.length; i += 8) {
              var point = matrix.transformPosition(new Vector3f(v[i], v[i + 1], v[i + 2]));
              boolean inArm =
                  java.lang.Math.abs(point.x) < MantisRig.ARM_WIDTH / 2
                      && point.y > -.125f
                      && point.y < .625f
                      && point.z > -depth / 2
                      && point.z < depth / 2;
              boolean inOpening =
                  java.lang.Math.abs(point.x) < MantisRig.ARM_WIDTH * MantisSkinPanels.WIDTH - .004f
                      && point.y > MantisSkinPanels.START
                      && point.y < MantisSkinPanels.END;
              if (age == 0)
                assertTrue(inArm, "The stowed implant must fit the actual skin cross section");
              assertFalse(
                  inArm && !inOpening,
                  () -> "Mechanism crosses intact arm skin: " + name + " " + point);
            }
          }
        }
    }
  }

  @Test
  void substantialHousingAndSupportHaveAShorterBalancedCuttingSection() throws Exception {
    var model = read("near");
    var axis =
        MantisArticulation.mount(1)
            .mul(MantisArticulation.support(1))
            .mul(MantisArticulation.blade(1, 1));
    var origin = axis.transformPosition(new Vector3f());
    axis.mul(MantisArticulation.tip(1)).mul(MantisArticulation.edge(1));
    float reach = 0;
    var edge = model.parts().get("edge").vertices();
    for (int i = 0; i < edge.length; i += 8)
      reach =
          java.lang.Math.max(
              reach,
              origin.distance(
                  axis.transformPosition(new Vector3f(edge[i], edge[i + 1], edge[i + 2]))));
    assertTrue(
        reach >= .90f && reach <= .99f,
        "Keep the cutting section shorter without shrinking the supporting implant");
    // The larger implant includes the base, linkage and receiver, not only the edge.
    var mount = model.parts().get("mount").vertices();
    float mx = 0;
    for (int i = 0; i < mount.length; i += 8)
      mx = java.lang.Math.max(mx, java.lang.Math.abs(mount[i]));
    assertTrue(mx * 2 >= .17f, "The mount must grow with the complete implant");
    assertTrue(MantisArticulation.SUPPORT_LENGTH >= .40f, "Keep the support full size");
    var receiver = model.parts().get("blade").vertices();
    float low = Float.POSITIVE_INFINITY, high = Float.NEGATIVE_INFINITY;
    for (int i = 2; i < receiver.length; i += 8) {
      low = java.lang.Math.min(low, receiver[i]);
      high = java.lang.Math.max(high, receiver[i]);
    }
    assertTrue(
        high - low >= .09f && high - low <= .105f, "The receiver needs a substantial side profile");
    var support = model.parts().get("support").vertices();
    float smin = Float.POSITIVE_INFINITY, smax = Float.NEGATIVE_INFINITY;
    for (int i = 2; i < support.length; i += 8) {
      smin = java.lang.Math.min(smin, support[i]);
      smax = java.lang.Math.max(smax, support[i]);
    }
    assertTrue(smax - smin >= .065f, "The support needs more depth as well as the housing");
  }

  @Test
  void emergingMechanismDoesNotCutThroughMovingDermalPlates() throws Exception {
    var model = read("near");
    for (float width : new float[] {3 / 16f, 4 / 16f}) {
      for (float age = 0; age <= 30; age += .5f) {
        var pose = MantisMotion.sample(true, age, 100, 0, false, 0, false);
        var elbow = MantisRig.elbow(pose);
        var bone = new Matrix4f(elbow).mul(MantisArticulation.mount(pose.blade(), width));
        for (String name : new String[] {"mount", "carriage", "support", "blade", "tip", "edge"}) {
          if (name.equals("carriage")) bone.mul(MantisArticulation.carriage(pose.slide()));
          if (name.equals("support"))
            bone.mul(MantisArticulation.support(pose.blade(), pose.extension()));
          if (name.equals("blade"))
            bone.mul(MantisArticulation.blade(pose.blade(), pose.extension()));
          if (name.equals("tip")) bone.mul(MantisArticulation.tip(pose.tip()));
          if (name.equals("edge")) bone.mul(MantisArticulation.edge(pose.tip()));
          var data = model.parts().get(name).vertices();
          for (var panel : MantisSkinPanels.ALL) {
            var plate =
                panel == MantisSkinPanels.Panel.PROXIMAL ? new Matrix4f() : new Matrix4f(elbow);
            plate.mul(MantisSkinPanels.transform(panel, width, pose.panels(), false));
            var relative = plate.invert().mul(bone);
            var region = MantisSkinPanels.region(panel, width);
            boolean side =
                panel == MantisSkinPanels.Panel.SIDE_LEFT
                    || panel == MantisSkinPanels.Panel.SIDE_RIGHT;
            float plane =
                side
                    ? (panel == MantisSkinPanels.Panel.SIDE_LEFT
                        ? -MantisRig.ARM_WIDTH / 2
                        : MantisRig.ARM_WIDTH / 2)
                    : -width / 2;
            var a = new Vector3f();
            var b = new Vector3f();
            var c = new Vector3f();
            var q = new Vector3f();
            for (int i = 0; i < data.length; i += 24) {
              relative.transformPosition(a.set(data[i], data[i + 1], data[i + 2]));
              relative.transformPosition(b.set(data[i + 8], data[i + 9], data[i + 10]));
              relative.transformPosition(c.set(data[i + 16], data[i + 17], data[i + 18]));
              for (int edge = 0; edge < 3; edge++) {
                var u = edge == 0 ? a : edge == 1 ? b : c;
                var v = edge == 0 ? b : edge == 1 ? c : a;
                float du = (side ? u.x : u.z) - plane, dv = (side ? v.x : v.z) - plane;
                if (du * dv >= 0
                    || java.lang.Math.abs(du) < .00001
                    || java.lang.Math.abs(dv) < .00001) continue;
                q.set(u).lerp(v, du / (du - dv));
                assertFalse(
                    region.contains(q.x, q.y, q.z),
                    "Moving cover collision: "
                        + panel
                        + " / "
                        + name
                        + " at "
                        + age
                        + " local "
                        + q);
              }
            }
          }
        }
      }
    }
  }

  @Test
  void rearwardRisingSupportAndForwardBladeShareTheSecondHinge() {
    for (float t = 0; t <= 1; t += .02f) {
      var lower = MantisArticulation.mount(t).mul(MantisArticulation.support(t));
      var expected = lower.transformPosition(new Vector3f(0, MantisArticulation.SUPPORT_LENGTH, 0));
      var upper = new Matrix4f(lower).mul(MantisArticulation.blade(t, t));
      assertEquals(0, expected.distance(upper.transformPosition(new Vector3f())), .000001);
    }
    var support = MantisArticulation.support(1);
    var lowerDirection = support.transformDirection(new Vector3f(0, 1, 0)).normalize();
    var upperDirection =
        new Matrix4f(support)
            .mul(MantisArticulation.blade(1, 1))
            .transformDirection(new Vector3f(0, 1, 0))
            .normalize();
    assertTrue(lowerDirection.y < -.7 && lowerDirection.z < -.5, "Support must go backward and up");
    assertTrue(upperDirection.y > .95, "Cutting section must point forward past the hand");
    assertTrue(
        lowerDirection.dot(upperDirection) < -.65,
        "Sections incorrectly form one straight arm-aligned rail");
  }
}
