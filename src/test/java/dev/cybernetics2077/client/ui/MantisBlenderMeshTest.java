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
  void optimizedMeshesFitBudgetsAndHaveValidUVsAndNormals() throws Exception {
    var near = read("near");
    var far = read("far");
    int n = near.parts().values().stream().mapToInt(MantisMeshData.Part::triangles).sum();
    int f = far.parts().values().stream().mapToInt(MantisMeshData.Part::triangles).sum();
    assertTrue(n < 6000 && n > 1000);
    assertTrue(f < 700 && f < n * .15);
    for (var model : new MantisMeshData[] {near, far})
      for (var part : model.parts().values()) {
        var v = part.vertices();
        assertEquals(part.triangles() * 4 * 8, v.length);
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
