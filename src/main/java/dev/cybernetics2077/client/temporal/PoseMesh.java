package dev.cybernetics2077.client.temporal;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.mixin.client.*;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/** CPU-only capture of the actual player renderer, including armor and articulated cyberarms. */
public final class PoseMesh implements MultiBufferSource {
  public record Batch(ResourceLocation texture, VertexFormat.Mode mode, float[] vertices) {}

  private static final int LIMIT = 24000;
  private final Map<RenderType, Recorder> recorders = new LinkedHashMap<>();
  private int vertices;

  @Override
  public VertexConsumer getBuffer(RenderType type) {
    return recorders.computeIfAbsent(
        type,
        t -> {
          ResourceLocation texture = null;
          if (t instanceof TemporalRenderTypeAccess access) {
            var state =
                ((TemporalTextureAccess) (Object) access.cybernetics$state()).cybernetics$texture();
            texture = ((TemporalTextureInvoker) state).cybernetics$location().orElse(null);
          }
          boolean format =
              t.format() == DefaultVertexFormat.NEW_ENTITY
                  || t.format() == DefaultVertexFormat.BLOCK;
          return new Recorder(format ? texture : null, t.mode());
        });
  }

  public List<Batch> finish() {
    var result = new ArrayList<Batch>();
    for (var r : recorders.values()) {
      r.commit();
      int primitive = r.mode == VertexFormat.Mode.TRIANGLES ? 3 : 4;
      int count = r.size / 8;
      count -= count % primitive;
      if (r.texture != null && count > 0)
        result.add(new Batch(r.texture, r.mode, Arrays.copyOf(r.data, count * 8)));
    }
    return List.copyOf(result);
  }

  private final class Recorder implements VertexConsumer {
    final ResourceLocation texture;
    final VertexFormat.Mode mode;
    float[] data = new float[1024];
    int size;
    boolean pending;
    float x, y, z, u, v, nx, ny, nz;

    Recorder(ResourceLocation texture, VertexFormat.Mode mode) {
      this.texture = texture;
      this.mode = mode;
    }

    void commit() {
      if (!pending) return;
      pending = false;
      if (texture == null || vertices >= LIMIT) return;
      if (size + 8 > data.length) data = Arrays.copyOf(data, data.length * 2);
      data[size++] = x;
      data[size++] = y;
      data[size++] = z;
      data[size++] = u;
      data[size++] = v;
      data[size++] = nx;
      data[size++] = ny;
      data[size++] = nz;
      vertices++;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
      commit();
      this.x = x;
      this.y = y;
      this.z = z;
      pending = true;
      return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
      this.u = u;
      this.v = v;
      return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
      return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
      return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
      nx = x;
      ny = y;
      nz = z;
      return this;
    }
  }
}
