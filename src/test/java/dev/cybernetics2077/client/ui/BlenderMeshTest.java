package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;

final class BlenderMeshTest {
  @Test
  void shippedBlenderMeshHasEveryBodySystemAndFiniteCoordinates() throws Exception {
    try (var input =
        getClass().getResourceAsStream("/assets/cybernetics2077/models/hologram/body.cyh")) {
      var model = DiagnosticModel.read(input);
      assertTrue(model.triangles > 50000 && model.triangles < 150000);
      assertEquals(26, model.groups.size());
      var systems = new HashSet<Integer>();
      var roles = new HashSet<Integer>();
      for (var group : model.groups) {
        systems.add(group.region());
        roles.add(group.role());
        assertEquals(group.vertices() * 8, group.data().length);
      }
      assertEquals(10, systems.size());
      assertEquals(12, roles.size());
    }
  }

  @Test
  void rejectsBadHeadersCountsAndTruncation() {
    assertThrows(
        IOException.class, () -> DiagnosticModel.read(new ByteArrayInputStream(new byte[0])));
    assertThrows(
        IOException.class, () -> DiagnosticModel.read(new ByteArrayInputStream(new byte[12])));
    for (int count : new int[] {0, 129, Integer.MAX_VALUE})
      assertThrows(
          IOException.class, () -> DiagnosticModel.read(new ByteArrayInputStream(header(count))));
    assertThrows(
        IOException.class, () -> DiagnosticModel.read(new ByteArrayInputStream(header(1))));
  }

  @Test
  void rejectsNonFiniteAndOutOfBodyVertices() throws Exception {
    for (float bad : new float[] {Float.NaN, Float.POSITIVE_INFINITY, 1000}) {
      var bytes = new ByteArrayOutputStream();
      var out = new DataOutputStream(bytes);
      out.write(header(1));
      out.writeUTF("bad");
      out.writeByte(0);
      out.writeByte(0);
      out.writeInt(3);
      for (int c = 0; c < 4; c++) out.writeFloat(1);
      for (int i = 0; i < 3; i++) {
        out.writeFloat(bad);
        out.writeFloat(0);
        out.writeFloat(0);
        out.writeFloat(0);
        out.writeFloat(0);
        out.writeFloat(-1);
        out.writeFloat(0);
        out.writeFloat(0);
      }
      assertThrows(
          IOException.class,
          () -> DiagnosticModel.read(new ByteArrayInputStream(bytes.toByteArray())));
    }
  }

  private static byte[] header(int count) throws IOException {
    var bytes = new ByteArrayOutputStream();
    var out = new DataOutputStream(bytes);
    out.writeInt(0x43594833);
    out.writeInt(1);
    out.writeInt(count);
    return bytes.toByteArray();
  }
}
