package dev.cybernetics2077.client.ui;

import java.io.*;
import java.util.*;

/**
 * Evaluated Blender mesh data. No geometry is synthesized by Java. GPU upload is renderer-owned.
 */
final class DiagnosticModel {
  record Group(String name, int role, int region, int vertices, float[] material, float[] data) {}

  final List<Group> groups;
  final int triangles;

  private DiagnosticModel(List<Group> groups, int triangles) {
    this.groups = List.copyOf(groups);
    this.triangles = triangles;
  }

  static DiagnosticModel read(InputStream stream) throws IOException {
    if (stream == null) throw new IOException("Missing Blender hologram mesh");
    var in = new DataInputStream(new BufferedInputStream(stream));
    if (in.readInt() != 0x43594833 || in.readInt() != 1)
      throw new IOException("Unknown hologram format");
    int count = in.readInt();
    if (count < 1 || count > 128) throw new IOException("Invalid hologram group count");
    List<Group> groups = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < count; i++) {
      String name = in.readUTF();
      int role = in.readUnsignedByte(), region = in.readUnsignedByte(), vertices = in.readInt();
      if (name.length() > 128
          || role > 11
          || region > 9
          || vertices < 3
          || vertices % 3 != 0
          || vertices > 900000
          || total + vertices > 1500000) throw new IOException("Invalid hologram group");
      float[] material = new float[4];
      for (int c = 0; c < 4; c++) {
        material[c] = finite(in);
        if (material[c] < 0 || material[c] > 1) throw new IOException("Invalid material");
      }
      float[] data = new float[vertices * 8];
      for (int n = 0; n < data.length; n++) data[n] = finite(in);
      for (int v = 0; v < vertices; v++) {
        int o = v * 8;
        float x = data[o],
            y = data[o + 1],
            z = data[o + 2],
            nx = data[o + 3],
            ny = data[o + 4],
            nz = data[o + 5];
        if (Math.abs(x) > .6
            || y < -.61
            || y > 1.61
            || Math.abs(z) > .4
            || Math.abs(nx * nx + ny * ny + nz * nz - 1) > .025)
          throw new IOException("Invalid anatomical coordinates/normals");
      }
      total += vertices;
      groups.add(new Group(name, role, region, vertices, material, data));
    }
    if (in.read() != -1) throw new IOException("Trailing hologram data");
    return new DiagnosticModel(groups, total / 3);
  }

  private static float finite(DataInputStream in) throws IOException {
    float v = in.readFloat();
    if (!Float.isFinite(v)) throw new IOException("Non-finite hologram attribute");
    return v;
  }
}
