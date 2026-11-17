package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import org.joml.Vector3f;

/** Original bevelled blades and articulated forearm shells, with the wearer's real skin UVs. */
public final class MantisMesh {
  private static final net.minecraft.resources.ResourceLocation METAL =
      Cybernetics2077.id("textures/entity/mantis_mechanism.png");
  private static final ByteBufferBuilder SKIN_MEMORY = new ByteBufferBuilder(16384),
      METAL_MEMORY = new ByteBufferBuilder(262144),
      FALLBACK_MEMORY = new ByteBufferBuilder(256);
  private static MultiBufferSource.BufferSource isolated;
  private static net.minecraft.resources.ResourceLocation currentSkin;

  private static MultiBufferSource.BufferSource source(
      net.minecraft.resources.ResourceLocation skin) {
    if (isolated == null || !skin.equals(currentSkin)) {
      var fixed = new java.util.LinkedHashMap<RenderType, ByteBufferBuilder>();
      fixed.put(RenderType.entityCutoutNoCull(skin), SKIN_MEMORY);
      fixed.put(RenderType.entityCutoutNoCull(METAL), METAL_MEMORY);
      isolated = MultiBufferSource.immediateWithBuffers(fixed, FALLBACK_MEMORY);
      currentSkin = skin;
    }
    return isolated;
  }

  private static final float[][] EDGE = {
    {0, 0},
    {.075f, .06f},
    {.12f, .32f},
    {.135f, .64f},
    {.10f, .93f},
    {.025f, 1.18f},
    {-.045f, 1.31f},
    {-.055f, 1.07f},
    {-.06f, .80f},
    {-.045f, .40f},
    {-.055f, .13f}
  };

  public static void render(
      PoseStack p,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      boolean left,
      MantisMotion.Pose motion) {
    boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
    float width = slim ? 3 / 16f : 4 / 16f;
    var local = source(player.getSkin().texture());
    var skin = local.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
    var metal = local.getBuffer(RenderType.entityCutoutNoCull(METAL));
    int u = left ? 32 : 40, v = left ? 48 : 16;
    float side = left ? 1 : -1;
    // Upper arm and fist remain intact. The lateral forearm casing opens along the outside.
    for (int layer = 0; layer < 2; layer++) {
      var flag =
          left
              ? net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE
              : net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE;
      if (layer == 1 && !player.isModelPartShown(flag)) continue;
      float inflate = layer == 0 ? 0 : .016f;
      int U = layer == 0 ? u : left ? 48 : 40, V = layer == 0 ? v : left ? 48 : 32;
      skinBox(
          p,
          skin,
          light,
          -width / 2 - inflate,
          -.125f - inflate,
          -.125f - inflate,
          width / 2 + inflate,
          .1875f,
          .125f + inflate,
          U,
          V,
          width,
          true,
          false);
      skinBox(
          p,
          skin,
          light,
          -width / 2 - inflate,
          .4375f,
          -.125f - inflate,
          width / 2 + inflate,
          .625f + inflate,
          .125f + inflate,
          U,
          V,
          width,
          false,
          true);
      // Inner half stays anchored; the outside half becomes the hinged hatch.
      skinBox(
          p,
          skin,
          light,
          side < 0 ? 0 : -width / 2 - inflate,
          .1875f,
          -.125f - inflate,
          side < 0 ? width / 2 + inflate : 0,
          .4375f,
          .125f + inflate,
          U,
          V,
          width,
          false,
          false);
      p.pushPose();
      p.translate(side * .025f * motion.panels(), .1875f, 0);
      p.mulPose(Axis.ZP.rotationDegrees(-side * motion.panels() * 32));
      p.translate(0, -.1875f, 0);
      skinBox(
          p,
          skin,
          light,
          side < 0 ? -width / 2 - inflate : 0,
          .1875f,
          -.125f - inflate,
          side < 0 ? 0 : width / 2 + inflate,
          .4375f,
          .125f + inflate,
          U,
          V,
          width,
          false,
          false);
      if (layer == 0) {
        // Recessed inner metal of the casing, plus small ceramic edge fasteners.
        box(p, metal, light, -.003f, .192f, -.119f, .003f, .433f, .119f, 0xff41525b);
        for (int k = 0; k < 4; k++) {
          float xx = side * (width / 2 + .004f);
          box(
              p,
              metal,
              light,
              xx - .008f,
              .222f + k * .05f,
              -.102f,
              xx + .008f,
              .234f + k * .05f,
              -.087f,
              0xffbfc5bb);
          box(
              p,
              metal,
              light,
              xx - .008f,
              .222f + k * .05f,
              .087f,
              xx + .008f,
              .234f + k * .05f,
              .102f,
              0xffbfc5bb);
        }
      }
      p.popPose();
    }
    // The exposed lateral rail replaces the skin-covered interior of the opened hatch.
    box(
        p,
        metal,
        light,
        side < 0 ? -.035f : -.004f,
        .195f,
        -.108f,
        side < 0 ? .004f : .035f,
        .432f,
        .108f,
        0xff28373f);
    // Cuff joint, sliding carriage and external piston only emerge once the skin panels separate.
    float open = motion.panels();
    if (open > .005) {
      p.pushPose();
      // Mirrored lateral mount: local -Z becomes the OUTER side of the actual arm.
      p.mulPose(Axis.YP.rotationDegrees(side < 0 ? 90 : -90));
      p.translate(0, 0, -.085 * open);
      cylinder(p, metal, light, 0, .25f, -.075f, .073f, .17f, 0xffa29167);
      cylinder(p, metal, light, 0, .25f, -.075f, .043f, .184f, 0xff252f38);
      box(p, metal, light, -.075f, .285f, -.12f, .075f, .43f, -.045f, 0xffa6aeac);
      box(p, metal, light, -.022f, .32f, -.142f, .022f, .52f, -.095f, 0xff454f57);
      for (int k = 0; k < 7; k++)
        box(
            p,
            metal,
            light,
            -.045f,
            .31f + k * .027f,
            -.127f,
            .045f,
            .316f + k * .027f,
            -.119f,
            0xffe0dfd2);
      for (int k = -1; k <= 1; k += 2) {
        box(
            p,
            metal,
            light,
            k * .072f - .012f,
            .25f,
            -.055f,
            k * .072f + .012f,
            .55f,
            -.033f,
            0xff647478);
        box(
            p,
            metal,
            light,
            k * .072f - .007f,
            .38f,
            -.061f,
            k * .072f + .007f,
            .595f,
            -.04f,
            0xffc9d4d3);
      }
      box(p, metal, 0xf000f0, -.027f, .36f, -.148f, .027f, .369f, -.142f, 0xffef6552);
      p.translate(0, .38, -.125);
      p.mulPose(Axis.XP.rotationDegrees(160 * (1 - motion.blade()) - 12));
      p.scale(1, .035f + .965f * motion.blade(), 1);
      blade(p, metal, light, left);
      p.popPose();
    }
    local.endBatch();
  }

  private static void blade(PoseStack p, VertexConsumer out, int light, boolean left) {
    // Every face has a distinct machined bevel; the point is an actual tapered mesh.
    float mirror = left ? -1 : 1;
    for (int i = 0; i < EDGE.length; i++) {
      var a = EDGE[i];
      var b = EDGE[(i + 1) % EDGE.length];
      quad(
          p,
          out,
          light,
          new float[] {mirror * a[0], a[1], -.010f},
          new float[] {mirror * b[0], b[1], -.010f},
          new float[] {mirror * b[0], b[1], .010f},
          new float[] {mirror * a[0], a[1], .010f},
          0xffedf3ef);
      for (int side : new int[] {-1, 1}) {
        float z = side * .010f;
        quad(
            p,
            out,
            light,
            new float[] {mirror * a[0], a[1], z},
            new float[] {mirror * b[0], b[1], z},
            new float[] {mirror * .025f, .61f, side * .032f},
            new float[] {mirror * .025f, .61f, side * .032f},
            side < 0 ? 0xffaebbbd : 0xff72838b);
      }
    }
    // Narrow fuller, serrated heel and a gold maker plate.
    box(p, out, light, -.013f, .12f, -.035f, .019f, .72f, -.031f, 0xff34444e);
    for (int i = 0; i < 8; i++)
      box(
          p,
          out,
          light,
          -.055f,
          .13f + i * .031f,
          -.015f,
          -.037f,
          .142f + i * .031f,
          .015f,
          0xffd0d6ce);
    box(p, out, light, -.035f, .055f, -.022f, .037f, .14f, .022f, 0xffab9c72);
  }

  private static void cylinder(
      PoseStack p,
      VertexConsumer out,
      int light,
      float x,
      float y,
      float z,
      float radius,
      float length,
      int color) {
    for (int k = 0; k < 32; k++) {
      float a = k * (float) Math.PI / 16, b = (k + 1) * (float) Math.PI / 16;
      float ay = y + (float) Math.cos(a) * radius, az = z + (float) Math.sin(a) * radius;
      float by = y + (float) Math.cos(b) * radius, bz = z + (float) Math.sin(b) * radius;
      quad(
          p,
          out,
          light,
          new float[] {x - length / 2, ay, az},
          new float[] {x - length / 2, by, bz},
          new float[] {x + length / 2, by, bz},
          new float[] {x + length / 2, ay, az},
          color);
      for (int s : new int[] {-1, 1})
        quad(
            p,
            out,
            light,
            new float[] {x + s * length / 2, y, z},
            new float[] {x + s * length / 2, ay, az},
            new float[] {x + s * length / 2, by, bz},
            new float[] {x + s * length / 2, y, z},
            s < 0 ? 0xff8c8269 : 0xffc8b88b);
    }
  }

  private static void box(
      PoseStack p,
      VertexConsumer out,
      int light,
      float x,
      float y,
      float z,
      float X,
      float Y,
      float Z,
      int color) {
    float[][] v = {
      {x, y, z}, {X, y, z}, {X, Y, z}, {x, Y, z}, {x, y, Z}, {X, y, Z}, {X, Y, Z}, {x, Y, Z}
    };
    for (int[] f :
        new int[][] {
          {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {0, 1, 5, 4}, {3, 7, 6, 2}
        }) quad(p, out, light, v[f[0]], v[f[1]], v[f[2]], v[f[3]], color);
  }

  private static void quad(
      PoseStack p,
      VertexConsumer out,
      int light,
      float[] a,
      float[] b,
      float[] c,
      float[] d,
      int color) {
    textured(p, out, light, a, b, c, d, color, .25f, .25f, .75f, .75f);
  }

  private static void textured(
      PoseStack p,
      VertexConsumer out,
      int light,
      float[] a,
      float[] b,
      float[] c,
      float[] d,
      int color,
      float u,
      float v,
      float U,
      float V) {
    var n =
        new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2])
            .cross(c[0] - a[0], c[1] - a[1], c[2] - a[2]);
    if (n.lengthSquared() < .0000001f) n.set(0, 0, -1);
    else n.normalize();
    float[][] points = {a, b, c, d};
    float[][] uv = {{u, v}, {u, V}, {U, V}, {U, v}};
    for (int i = 0; i < 4; i++)
      out.addVertex(p.last(), points[i][0], points[i][1], points[i][2])
          .setColor(color)
          .setUv(uv[i][0], uv[i][1])
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(light)
          .setNormal(p.last(), n.x, n.y, n.z);
  }

  private static void skinBox(
      PoseStack p,
      VertexConsumer out,
      int light,
      float x,
      float y,
      float z,
      float X,
      float Y,
      float Z,
      int u,
      int v,
      float width,
      boolean top,
      boolean bottom) {
    float w = width * 16;
    float xa = Math.clamp((x + width / 2) * 16, 0, w), xb = Math.clamp((X + width / 2) * 16, 0, w);
    float start = v + 4 + (y + .125f) * 16, end = v + 4 + (Y + .125f) * 16;
    float za = (z + .125f) * 16, zb = (Z + .125f) * 16;
    // UV ordering follows the vanilla arm's unfolded cuboid. No skin-colour approximation.
    if (z < -.12)
      textured(
          p,
          out,
          light,
          new float[] {x, y, z},
          new float[] {x, Y, z},
          new float[] {X, Y, z},
          new float[] {X, y, z},
          -1,
          (u + 4 + xa) / 64f,
          start / 64,
          (u + 4 + xb) / 64f,
          end / 64);
    if (Z > .12)
      textured(
          p,
          out,
          light,
          new float[] {X, y, Z},
          new float[] {X, Y, Z},
          new float[] {x, Y, Z},
          new float[] {x, y, Z},
          -1,
          (u + 8 + 2 * w - xb) / 64f,
          start / 64,
          (u + 8 + 2 * w - xa) / 64f,
          end / 64);
    textured(
        p,
        out,
        light,
        new float[] {x, y, Z},
        new float[] {x, Y, Z},
        new float[] {x, Y, z},
        new float[] {x, y, z},
        -1,
        (u + 4 - zb) / 64f,
        start / 64,
        (u + 4 - za) / 64f,
        end / 64);
    textured(
        p,
        out,
        light,
        new float[] {X, y, z},
        new float[] {X, Y, z},
        new float[] {X, Y, Z},
        new float[] {X, y, Z},
        -1,
        (u + 4 + w + za) / 64f,
        start / 64,
        (u + 4 + w + zb) / 64f,
        end / 64);
    if (top)
      textured(
          p,
          out,
          light,
          new float[] {x, y, Z},
          new float[] {x, y, z},
          new float[] {X, y, z},
          new float[] {X, y, Z},
          -1,
          (u + 4) / 64f,
          v / 64f,
          (u + 4 + w) / 64f,
          (v + 4) / 64f);
    if (bottom)
      textured(
          p,
          out,
          light,
          new float[] {x, Y, z},
          new float[] {x, Y, Z},
          new float[] {X, Y, Z},
          new float[] {X, Y, z},
          -1,
          (u + 4 + w) / 64f,
          v / 64f,
          (u + 4 + 2 * w) / 64f,
          (v + 4) / 64f);
  }

  private MantisMesh() {}
}
