package dev.cybernetics2077.client.temporal;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/** Upload each captured pose once; replaying a trail does not rebuild its vertices on the CPU. */
final class TemporalMesh implements AutoCloseable {
  private static final ByteBufferBuilder MEMORY = new ByteBufferBuilder(1024 * 1024);
  final RenderType type;
  final int count;
  private final VertexBuffer gpu;

  TemporalMesh(PoseMesh.Batch batch) {
    type = TemporalMaterial.type(batch.texture(), batch.mode());
    count = batch.vertices().length / 8;
    var builder = new BufferBuilder(MEMORY, batch.mode(), DefaultVertexFormat.NEW_ENTITY);
    var v = batch.vertices();
    for (int i = 0; i < v.length; i += 8)
      builder
          .addVertex(v[i], v[i + 1], v[i + 2])
          .setColor(-1)
          .setUv(v[i + 3], v[i + 4])
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(15728640)
          .setNormal(v[i + 5], v[i + 6], v[i + 7]);
    gpu = new VertexBuffer(VertexBuffer.Usage.STATIC);
    gpu.bind();
    gpu.upload(builder.buildOrThrow());
    VertexBuffer.unbind();
  }

  void draw(PoseStack pose, int color, net.minecraft.world.phys.AABB exclude) {
    type.setupRenderState();
    RenderSystem.setShaderColor(
        ((color >> 16) & 255) / 255f,
        ((color >> 8) & 255) / 255f,
        (color & 255) / 255f,
        ((color >>> 24) & 255) / 255f);
    var shader = RenderSystem.getShader();
    shader.safeGetUniform("ExclusionEnabled").set(exclude == null ? 0f : 1f);
    if (exclude != null) {
      shader
          .safeGetUniform("ExclusionMin")
          .set((float) exclude.minX, (float) exclude.minY, (float) exclude.minZ);
      shader
          .safeGetUniform("ExclusionMax")
          .set((float) exclude.maxX, (float) exclude.maxY, (float) exclude.maxZ);
    }
    gpu.bind();
    gpu.drawWithShader(
        new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose()),
        RenderSystem.getProjectionMatrix(),
        RenderSystem.getShader());
    VertexBuffer.unbind();
    RenderSystem.setShaderColor(1, 1, 1, 1);
    type.clearRenderState();
  }

  @Override
  public void close() {
    gpu.close();
  }
}
