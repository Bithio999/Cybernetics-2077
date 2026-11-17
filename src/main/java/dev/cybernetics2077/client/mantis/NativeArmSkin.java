package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Captures native cube UVs once, then clips faces without stretching or replacing the wearer's
 * skin.
 */
final class NativeArmSkin {
  record V(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    V lerp(V b, float t) {
      return new V(
          x + (b.x - x) * t,
          y + (b.y - y) * t,
          z + (b.z - z) * t,
          u + (b.u - u) * t,
          v + (b.v - v) * t,
          nx,
          ny,
          nz);
    }

    float axis(int a) {
      return a == 0 ? x : a == 1 ? y : z;
    }
  }

  private static final Map<Integer, List<List<V>>> CACHE = new HashMap<>();

  private record Bounds(float x, float X, float y, float Y, float z, float Z) {}

  private static final Map<List<V>, Map<Bounds, List<V>>> PATCHES = new IdentityHashMap<>();
  private static final Map<List<V>, Map<MantisSkinPanels.Region, List<V>>> REGIONS =
      new IdentityHashMap<>();

  static List<List<V>> mesh(
      AbstractClientPlayer player, boolean left, boolean sleeve, boolean slim) {
    int key = (left ? 1 : 0) | (sleeve ? 2 : 0) | (slim ? 4 : 0);
    return CACHE.computeIfAbsent(
        key,
        k -> {
          var model =
              ((PlayerRenderer)
                      Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player))
                  .getModel();
          ModelPart part =
              sleeve
                  ? (left ? model.leftSleeve : model.rightSleeve)
                  : (left ? model.leftArm : model.rightArm);
          var collector = new Collector(MantisRig.centerOffset(left, slim));
          var identity = new PoseStack();
          part.visit(
              new PoseStack(),
              (pose, path, index, cube) -> cube.compile(identity.last(), collector, 0, 0, -1));
          List<List<V>> faces = new ArrayList<>();
          for (int i = 0; i < collector.vertices.size(); i += 4)
            faces.add(List.copyOf(collector.vertices.subList(i, i + 4)));
          return List.copyOf(faces);
        });
  }

  static List<V> clip(List<V> poly, int axis, float boundary, boolean greater) {
    if (poly.isEmpty()) return poly;
    List<V> out = new ArrayList<>();
    V a = poly.getLast();
    boolean ain = (a.axis(axis) - boundary) * (greater ? 1 : -1) >= -.000001f;
    for (var b : poly) {
      boolean bin = (b.axis(axis) - boundary) * (greater ? 1 : -1) >= -.000001f;
      if (ain != bin) out.add(a.lerp(b, (boundary - a.axis(axis)) / (b.axis(axis) - a.axis(axis))));
      if (bin) out.add(b);
      a = b;
      ain = bin;
    }
    return out;
  }

  static void part(
      PoseStack p,
      VertexConsumer out,
      int light,
      List<V> face,
      float x,
      float X,
      float y,
      float Y) {
    part(p, out, light, face, x, X, y, Y, -1, 1);
  }

  static void part(
      PoseStack p,
      VertexConsumer out,
      int light,
      List<V> face,
      float x,
      float X,
      float y,
      float Y,
      float z,
      float Z) {
    // These rectangles only depend on arm width and hatch geometry, not on the
    // animation. Do not clip skin polygons and allocate their vertices each frame.
    var poly =
        PATCHES
            .computeIfAbsent(face, ignored -> new HashMap<>())
            .computeIfAbsent(
                new Bounds(x, X, y, Y, z, Z),
                ignored ->
                    List.copyOf(
                        clip(
                            clip(
                                clip(
                                    clip(clip(clip(face, 0, x, true), 0, X, false), 1, y, true),
                                    1,
                                    Y,
                                    false),
                                2,
                                z,
                                true),
                            2,
                            Z,
                            false)));
    draw(p, out, light, poly);
  }

  private static void draw(PoseStack p, VertexConsumer out, int light, List<V> poly) {
    for (int i = 1; i + 1 < poly.size(); i++) {
      vertex(p, out, light, poly.getFirst());
      vertex(p, out, light, poly.get(i));
      vertex(p, out, light, poly.get(i + 1));
      vertex(p, out, light, poly.get(i + 1));
    }
  }

  private static List<V> region(List<V> face, MantisSkinPanels.Region r) {
    return REGIONS
        .computeIfAbsent(face, ignored -> new HashMap<>())
        .computeIfAbsent(
            r,
            ignored -> {
              var poly =
                  clip(
                      clip(
                          clip(
                              clip(
                                  clip(clip(face, 0, r.x(), true), 0, r.X(), false),
                                  1,
                                  r.y(),
                                  true),
                              1,
                              r.Y(),
                              false),
                          2,
                          r.z(),
                          true),
                      2,
                      r.Z(),
                      false);
              if (r.split() != 0 && !poly.isEmpty()) {
                List<V> cut = new ArrayList<>();
                V a = poly.getLast();
                float da = (a.x - r.slope() * a.y - r.intercept()) * r.split();
                for (var b : poly) {
                  float db = (b.x - r.slope() * b.y - r.intercept()) * r.split();
                  if ((da >= 0) != (db >= 0)) cut.add(a.lerp(b, da / (da - db)));
                  if (db >= 0) cut.add(b);
                  a = b;
                  da = db;
                }
                poly = cut;
              }
              return List.copyOf(poly);
            });
  }

  static void panel(
      PoseStack p,
      VertexConsumer skin,
      VertexConsumer metal,
      int light,
      List<V> face,
      MantisSkinPanels.Region r,
      boolean solid) {
    var poly = region(face, r);
    draw(p, skin, light, poly);
    if (!solid || poly.size() < 3) return;
    for (int i = 1; i + 1 < poly.size(); i++) {
      backing(p, metal, light, poly.getFirst(), true);
      backing(p, metal, light, poly.get(i + 1), true);
      backing(p, metal, light, poly.get(i), true);
      backing(p, metal, light, poly.get(i), true);
    }
    for (int i = 0; i < poly.size(); i++) {
      var a = poly.get(i);
      var b = poly.get((i + 1) % poly.size());
      backing(p, metal, light, a, false);
      backing(p, metal, light, b, false);
      backing(p, metal, light, b, true);
      backing(p, metal, light, a, true);
    }
  }

  private static void backing(PoseStack p, VertexConsumer out, int light, V v, boolean inner) {
    float d = inner ? -.004f : 0;
    out.addVertex(p.last(), v.x + v.nx * d, v.y + v.ny * d, v.z + v.nz * d)
        .setColor(0xff46545b)
        .setUv(.5f, .5f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(p.last(), -v.nx, -v.ny, -v.nz);
  }

  private static void vertex(PoseStack p, VertexConsumer out, int light, V v) {
    out.addVertex(p.last(), v.x, v.y, v.z)
        .setColor(-1)
        .setUv(v.u, v.v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(p.last(), v.nx, v.ny, v.nz);
  }

  private static final class Collector implements VertexConsumer {
    final List<V> vertices = new ArrayList<>();
    final float center;
    float x, y, z, u, v;

    Collector(float center) {
      this.center = center;
    }

    public VertexConsumer addVertex(float x, float y, float z) {
      this.x = x - center;
      this.y = y;
      this.z = z;
      return this;
    }

    public VertexConsumer setColor(int r, int g, int b, int a) {
      return this;
    }

    public VertexConsumer setUv(float u, float v) {
      this.u = u;
      this.v = v;
      return this;
    }

    public VertexConsumer setUv1(int u, int v) {
      return this;
    }

    public VertexConsumer setUv2(int u, int v) {
      return this;
    }

    public VertexConsumer setNormal(float x, float y, float z) {
      vertices.add(new V(this.x, this.y, this.z, u, v, x, y, z));
      return this;
    }
  }
}
