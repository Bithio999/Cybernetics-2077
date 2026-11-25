package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.MantisMeshData;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

final class MantisVariantMeshTest {
  @Test
  void variantsHaveDistinctHardwareAndBoundedLodsWithValidAtlasCoordinates() throws Exception {
    var paths = Path.of("src/main/resources/assets/cybernetics2077/meshes/mantis");
    var meshes = new java.util.HashSet<Integer>();
    for (var variant : new String[] {"thermal", "electrical", "toxic"}) {
      int near = 0, far = 0;
      for (var lod : new String[] {"near", "far"}) {
        try (var in = Files.newInputStream(paths.resolve(variant + "_" + lod + ".cymesh"))) {
          var model = MantisMeshData.read(in);
          int tris = 0;
          for (var part : model.parts().values()) {
            tris += part.triangles();
            var v = part.vertices();
            for (int i = 0; i < v.length; i += 8) {
              assertTrue(v[i + 3] >= 0 && v[i + 3] <= 1);
              assertTrue(v[i + 4] >= 0 && v[i + 4] <= 1);
              assertTrue(Math.abs(v[i]) < .1);
            }
          }
          if (lod.equals("near")) near = tris;
          else far = tris;
        }
      }
      assertTrue(near <= 4200);
      assertTrue(far <= 1000 && far < near / 3);
      meshes.add(near);
    }
    assertEquals(
        3, meshes.size(), "Variants need different geometry, not identical recolored meshes");
  }
}
