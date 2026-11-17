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

  public static void render(
      PoseStack p,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      boolean left,
      MantisMotion.Pose motion) {
    render(p, buffers, light, player, left, motion, false);
  }

  public static void render(
      PoseStack p,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      boolean left,
      MantisMotion.Pose motion,
      boolean firstPerson) {
    boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
    float width = slim ? 3 / 16f : 4 / 16f;
    var local = source(player.getSkin().texture());
    var skin = local.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
    var metal = local.getBuffer(RenderType.entityCutoutNoCull(METAL));
    int u = left ? 32 : 40, v = left ? 48 : 16;
    float side = left ? 1 : -1;
    var sleeveFlag =
        left
            ? net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE
            : net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE;
    float window = width * .32f;
    // Native outer faces are preserved everywhere except the shallow dorsal recess.
    for (int layer = 0; layer < 2; layer++) {
      if (layer == 1 && !player.isModelPartShown(sleeveFlag)) continue;
      var faces = NativeArmSkin.mesh(player, left, layer == 1, slim);
      for (var face : faces) NativeArmSkin.part(p, skin, light, face, -1, 1, -1, MantisRig.ELBOW_Y);
    }
    if (motion.ready() > .001f)
      box(
          p,
          metal,
          light,
          -width / 2 + .002f,
          .235f,
          -.123f,
          width / 2 - .002f,
          .25f,
          .123f,
          0xff445158);
    p.pushPose();
    p.mulPose(MantisRig.elbow(motion));
    for (int layer = 0; layer < 2; layer++) {
      if (layer == 1 && !player.isModelPartShown(sleeveFlag)) continue;
      var faces = NativeArmSkin.mesh(player, left, layer == 1, slim);
      float z = -.125f - (layer == 0 ? 0 : .015625f);
      for (var face : faces) {
        if (face.getFirst().nz() > -.5f) {
          NativeArmSkin.part(p, skin, light, face, -1, 1, .25f, 1);
          continue;
        }
        // Four strips retain the original arm silhouette around the hatch.
        NativeArmSkin.part(p, skin, light, face, -1, -window, .25f, 1);
        NativeArmSkin.part(p, skin, light, face, window, 1, .25f, 1);
        NativeArmSkin.part(p, skin, light, face, -window, window, .25f, .29f);
        NativeArmSkin.part(p, skin, light, face, -window, window, .48f, 1);
        for (int flap : new int[] {-1, 1}) {
          float edge = flap * window;
          p.pushPose();
          p.translate(edge, 0, z);
          p.mulPose(Axis.YP.rotationDegrees(-flap * motion.panels() * 120));
          p.translate(-edge, 0, -z);
          NativeArmSkin.part(
              p, skin, light, face, flap < 0 ? -window : 0, flap < 0 ? 0 : window, .29f, .48f);
          if (layer == 0 && motion.panels() > .001f)
            box(
                p,
                metal,
                light,
                flap < 0 ? -window + .002f : .002f,
                .293f,
                z + .0015f,
                flap < 0 ? -.002f : window - .002f,
                .477f,
                z + .004f,
                0xff3a484e);
          p.popPose();
        }
      }
    }
    if (motion.panels() > .001f) mechanism(p, metal, light, width, side, motion, left);
    p.popPose();
    local.endBatch();
  }

  /** A backed dorsal recess: palm, sides, wrist and arm interior remain opaque. */
  private static void mechanism(
      PoseStack p,
      VertexConsumer out,
      int light,
      float width,
      float side,
      MantisMotion.Pose m,
      boolean left) {
    float w = width * .32f;
    // A solid volume seals the complete palm side. The recess is only 0.02 blocks deep.
    box(
        p,
        out,
        light,
        -width / 2 + .002f,
        .25f,
        -.105f,
        width / 2 - .002f,
        .50f,
        .123f,
        0xff293840);
    box(p, out, light, -w - .005f, .29f, -.123f, -w + .005f, .48f, -.105f, 0xff677d81);
    box(p, out, light, w - .005f, .29f, -.123f, w + .005f, .48f, -.105f, 0xff677d81);
    box(p, out, light, -w, .475f, -.123f, w, .49f, -.105f, 0xff677d81);
    box(p, out, light, -w + .009f, .299f, -.109f, w - .009f, .471f, -.106f, 0xff315c53);
    for (int k = 0; k < 3; k++) {
      float y = .308f + k * .051f;
      box(p, out, light, -.023f, y, -.119f, .023f, y + .031f, -.110f, 0xff172a31);
      for (int j = 0; j < 3; j++)
        box(
            p,
            out,
            light,
            -.033f,
            y + .004f + j * .009f,
            -.120f,
            .033f,
            y + .007f + j * .009f,
            -.113f,
            0xffc1ac75);
    }
    for (int wire = 0; wire < 4; wire++) {
      float x = (wire - 1.5f) * w * .40f, lastX = x, lastY = .30f, lastZ = -.12f;
      for (int j = 1; j <= 7; j++) {
        float t = j / 7f,
            xx = x + .006f * (float) Math.sin(t * Math.PI),
            yy = .30f + .165f * t,
            zz = -.12f - .006f * (float) Math.sin(t * Math.PI);
        rod(
            p,
            out,
            light,
            lastX,
            lastY,
            lastZ,
            xx,
            yy,
            zz,
            .003f,
            new int[] {0xffab5448, 0xff86a59b, 0xffbcaa70, 0xff45565f}[wire]);
        lastX = xx;
        lastY = yy;
        lastZ = zz;
      }
    }
    // Dorsal mount (negative model Z). At ready, the mount faces up; blade axis follows the hand.
    p.pushPose();
    p.translate(0, .35, -.135 - .015 * m.panels());
    cylinder(p, out, light, 0, 0, 0, .031f, .11f, 0xff99875e);
    cylinder(p, out, light, 0, 0, 0, .017f, .122f, 0xff24353e);
    p.mulPose(Axis.XP.rotationDegrees(155 * (1 - m.blade())));
    for (int k : new int[] {-1, 1}) {
      box(p, out, light, k * .033f - .012f, 0, -.023f, k * .033f + .012f, .19f, .023f, 0xff475b63);
      box(
          p,
          out,
          light,
          k * .033f - .006f,
          .12f,
          -.01f,
          k * .033f + .006f,
          .20f + .12f * m.extension(),
          .01f,
          0xffc8d3c8);
      rod(
          p,
          out,
          light,
          k * .047f,
          .02f,
          .015f,
          k * .047f,
          .20f + .12f * m.extension(),
          .015f,
          .004f,
          0xffc1c7b6);
    }
    box(p, out, light, -.02f, .03f, -.031f, .02f, .14f, .03f, 0xff938267);
    p.translate(0, .20 + .12 * m.extension(), 0);
    cylinder(p, out, light, 0, 0, 0, .025f, .10f, 0xff929f98);
    // Cutting plane stands on the dorsal mount, while its long axis remains directed past the hand.
    p.mulPose(Axis.YP.rotationDegrees(90));
    blade(p, out, light, m.tip());
    p.popPose();
  }

  private static void rod(
      PoseStack p,
      VertexConsumer out,
      int light,
      float x,
      float y,
      float z,
      float X,
      float Y,
      float Z,
      float radius,
      int color) {
    var delta = new Vector3f(X - x, Y - y, Z - z);
    float length = delta.length();
    if (length < .00001f) return;
    p.pushPose();
    p.translate(x, y, z);
    p.mulPose(new org.joml.Quaternionf().rotationTo(new Vector3f(0, 1, 0), delta.div(length)));
    for (int k = 0; k < 8; k++) {
      float a = k * (float) Math.PI / 4, b = (k + 1) * (float) Math.PI / 4;
      float ax = (float) Math.cos(a) * radius,
          az = (float) Math.sin(a) * radius,
          bx = (float) Math.cos(b) * radius,
          bz = (float) Math.sin(b) * radius;
      quad(
          p,
          out,
          light,
          new float[] {ax, 0, az},
          new float[] {bx, 0, bz},
          new float[] {bx, length, bz},
          new float[] {ax, length, az},
          color);
    }
    p.popPose();
  }

  private static final float[][] BLADE_SLEEVE = {
    {-.045f, 0}, {.065f, .025f}, {.105f, .17f}, {.10f, .55f}, {-.045f, .55f}, {-.055f, .07f}
  };
  private static final float[][] BLADE_TIP = {
    {-.032f, 0}, {.079f, 0}, {.085f, .15f}, {.048f, .36f}, {-.025f, .535f}, {-.037f, .42f}
  };

  private static void blade(PoseStack p, VertexConsumer out, int light, float tip) {
    // The rigid tip slides INSIDE a fixed, opaque sleeve. It never scales or stretches.
    p.pushPose();
    p.translate(0, .005 + .49 * tip, 0);
    bladePiece(p, out, light, BLADE_TIP, .012f, 0xffb5c7c9, 0xffe0e6dc);
    box(p, out, light, -.009f, .025f, -.014f, .007f, .35f, .014f, 0xff667f89);
    p.popPose();
    bladePiece(p, out, light, BLADE_SLEEVE, .035f, 0xff526873, 0xffa3b7bc);
    for (int face : new int[] {-1, 1}) {
      float z = face * .037f;
      box(p, out, light, -.02f, .10f, z - .002f, -.006f, .51f, z + .002f, 0xff20343d);
      for (int k = 0; k < 5; k++) {
        float y = .16f + k * .062f;
        box(p, out, light, .015f, y, z - .002f, .065f, y + .026f, z + .002f, 0xff1d3038);
        box(p, out, light, .015f, y, z - .003f, .065f, y + .004f, z + .003f, 0xffa3b9b9);
      }
      box(p, out, light, -.025f, .05f, z - .003f, .047f, .105f, z + .003f, 0xffb19b65);
      for (int k = 0; k < 4; k++)
        box(
            p,
            out,
            light,
            -.013f + k * .012f,
            .065f,
            z - .004f,
            -.008f + k * .012f,
            .088f,
            z + .004f,
            0xff33444b);
      // A visible collar makes the sliding tip's overlap/seam readable.
      box(p, out, light, -.045f, .515f, z - .004f, .10f, .539f, z + .004f, 0xff7d959e);
    }
  }

  private static void bladePiece(
      PoseStack p,
      VertexConsumer out,
      int light,
      float[][] profile,
      float thickness,
      int face,
      int edge) {
    for (int i = 0; i < profile.length; i++) {
      var a = profile[i];
      var b = profile[(i + 1) % profile.length];
      quad(
          p,
          out,
          light,
          new float[] {a[0], a[1], -thickness},
          new float[] {b[0], b[1], -thickness},
          new float[] {b[0], b[1], thickness},
          new float[] {a[0], a[1], thickness},
          edge);
      for (int sign : new int[] {-1, 1}) {
        quad(
            p,
            out,
            light,
            new float[] {a[0], a[1], sign * thickness},
            new float[] {b[0], b[1], sign * thickness},
            new float[] {.02f, .25f, sign * (thickness + .002f)},
            new float[] {.02f, .25f, sign * (thickness + .002f)},
            face);
      }
    }
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

  private MantisMesh() {}
}
