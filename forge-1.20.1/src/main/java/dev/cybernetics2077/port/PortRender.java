package dev.cybernetics2077.port;

import com.mojang.blaze3d.vertex.*;

public final class PortRender {
  public static void multiply(PoseStack pose, org.joml.Matrix4f matrix) {
    pose.last().pose().mul(matrix);
    pose.last().normal().mul(new org.joml.Matrix3f(matrix).invert().transpose());
  }

  public static BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) {
    return begin(Tesselator.getInstance().getBuilder(), mode, format);
  }

  public static BufferBuilder begin(
      BufferBuilder builder, VertexFormat.Mode mode, VertexFormat format) {
    builder.begin(mode, format);
    return builder;
  }
}
