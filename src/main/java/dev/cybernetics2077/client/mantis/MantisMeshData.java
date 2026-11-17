package dev.cybernetics2077.client.mantis;

import java.io.*;
import java.util.*;

/**
 * Compact Blender export, decoded once into render-ready primitive arrays. No runtime modelling.
 */
public record MantisMeshData(Map<String, Part> parts) {
  public record Part(float[] vertices, int triangles) {}

  public static MantisMeshData read(InputStream source) throws IOException {
    var in = new DataInputStream(source);
    if (in.readInt() != 0x43594d42 || in.readInt() != 1)
      throw new IOException("Unsupported Mantis mesh");
    int count = in.readInt();
    if (count != 4) throw new IOException("Expected four rigid Mantis groups");
    var parts = new LinkedHashMap<String, Part>();
    for (int p = 0; p < count; p++) {
      String name = in.readUTF();
      int vertices = in.readInt(), indices = in.readInt();
      if (vertices < 1 || vertices > 100000 || indices < 3 || indices > 150000 || indices % 3 != 0)
        throw new IOException("Invalid mesh size");
      var raw = new float[vertices * 8];
      for (int i = 0; i < raw.length; i++) {
        raw[i] = in.readFloat();
        if (!Float.isFinite(raw[i])) throw new IOException("Non-finite vertex");
      }
      var expanded = new float[indices / 3 * 4 * 8];
      int offset = 0;
      for (int i = 0; i < indices; i += 3) {
        int[] tri = {in.readInt(), in.readInt(), in.readInt()};
        for (int index : tri)
          if (index < 0 || index >= vertices) throw new IOException("Invalid triangle index");
        for (int corner : new int[] {0, 1, 2, 2}) {
          System.arraycopy(raw, tri[corner] * 8, expanded, offset, 8);
          offset += 8;
        }
      }
      if (parts.put(name, new Part(expanded, indices / 3)) != null)
        throw new IOException("Duplicate rigid group");
    }
    if (!parts.keySet().equals(Set.of("mount", "support", "blade", "tip")) || in.read() != -1)
      throw new IOException("Invalid Mantis groups or trailing data");
    return new MantisMeshData(Collections.unmodifiableMap(parts));
  }
}
