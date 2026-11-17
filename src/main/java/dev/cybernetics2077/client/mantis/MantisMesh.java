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
    // Upper arm rotates only at the shoulder; its end and the forearm share one elbow pivot.
    var sleeveFlag =
        left
            ? net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE
            : net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE;
    for (int layer = 0; !firstPerson && layer < 2; layer++) {
      if (layer == 1 && !player.isModelPartShown(sleeveFlag)) continue;
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
          .25f,
          .125f + inflate,
          U,
          V,
          width,
          true,
          true);
    }
    // Skin-covered upper/lower halves meet at a true central elbow, without a detached ball joint.
    p.pushPose();
    p.mulPose(MantisRig.elbow(motion));
    for (int layer = 0; layer < 2; layer++) {
      if (layer == 1 && !player.isModelPartShown(sleeveFlag)) continue;
      float inf = layer == 0 ? 0 : .016f;
      int U = layer == 0 ? u : left ? 48 : 40, V = layer == 0 ? v : left ? 48 : 32;
      skinBox(
          p,
          skin,
          light,
          -width / 2 - inf,
          .50f,
          -.125f - inf,
          width / 2 + inf,
          .625f + inf,
          .125f + inf,
          U,
          V,
          width,
          false,
          true);
      skinBox(
          p,
          skin,
          light,
          side < 0 ? 0 : -width / 2 - inf,
          .25f,
          -.125f - inf,
          side < 0 ? width / 2 + inf : 0,
          .50f,
          .125f + inf,
          U,
          V,
          width,
          false,
          false);
      for (int plate = 0; plate < 2; plate++) {
        float y = .25f + plate * .125f;
        p.pushPose();
        p.translate(side * width * .46f, y, 0);
        p.mulPose(Axis.YP.rotationDegrees(side * motion.panels() * (plate == 0 ? 95 : 110)));
        p.translate(-side * width * .46f, -y, 0);
        skinBox(
            p,
            skin,
            light,
            side < 0 ? -width / 2 - inf : width / 2 - .025f,
            y,
            -.125f - inf,
            side < 0 ? -width / 2 + .025f : width / 2 + inf,
            y + .125f,
            .125f + inf,
            U,
            V,
            width,
            false,
            false);
        if (layer == 0) {
          float inner = side * (width / 2 - .026f);
          box(
              p,
              metal,
              light,
              inner - .003f,
              y + .006f,
              -.108f,
              inner + .003f,
              y + .119f,
              .108f,
              0xff3e4c51);
          for (int k = 0; k < 3; k++) {
            float xx = side * width * .43f;
            box(
                p,
                metal,
                light,
                xx - .009f,
                y + .014f + k * .034f,
                -.13f,
                xx + .009f,
                y + .025f + k * .034f,
                -.115f,
                0xffd1d6c5);
          }
        }
        p.popPose();
      }
    }
    if (motion.panels() > .002f) mechanism(p, metal, light, width, side, motion, left);
    p.popPose();
    local.endBatch();
  }

  /** Open cavity, two sliding rails, exposed wiring and a folding external parallelogram. */
  private static void mechanism(
      PoseStack p,
      VertexConsumer out,
      int light,
      float width,
      float side,
      MantisMotion.Pose m,
      boolean left) {
    float open = m.panels(), extension = m.extension();
    // A recessed floor and perimeter rails leave the front OPEN; a closed box hid the internals.
    box(p, out, light, -.085f, .252f, -.106f, .085f, .496f, -.081f, 0xff1e2e35);
    for (int k = -1; k <= 1; k += 2)
      box(
          p,
          out,
          light,
          k * .078f - .007f,
          .252f,
          -.081f,
          k * .078f + .007f,
          .496f,
          .085f,
          0xff465860);
    box(p, out, light, -.063f, .273f, -.079f, .063f, .465f, -.059f, 0xff2f5b55);
    for (int chip = 0; chip < 3; chip++) {
      float y = .283f + chip * .057f;
      box(p, out, light, -.022f, y, -.06f, .022f, y + .035f, -.047f, 0xff192b32);
      for (int pin = 0; pin < 3; pin++) {
        float yy = y + .004f + pin * .011f;
        box(p, out, light, -.031f, yy, -.059f, .031f, yy + .004f, -.052f, 0xffc1a667);
      }
      box(p, out, light, -.043f, y + .012f, -.061f, -.034f, y + .023f, -.043f, 0xff93d5c3);
    }
    box(
        p,
        out,
        light,
        side < 0 ? -.095f : .045f,
        .269f,
        -.084f,
        side < 0 ? -.045f : .095f,
        .478f,
        .084f,
        0xff314c51);
    for (int k = -1; k <= 1; k += 2) {
      rod(
          p,
          out,
          light,
          side * .07f,
          .26f,
          k * .084f,
          side * .07f,
          .49f,
          k * .084f,
          .009f,
          0xffbac6bf);
      rod(
          p,
          out,
          light,
          side * .09f,
          .28f,
          k * .061f,
          side * (.13f + .05f * open),
          .46f,
          k * .061f,
          .013f,
          0xff515e62);
      rod(
          p,
          out,
          light,
          side * .09f,
          .28f,
          k * .061f,
          side * (.13f + .05f * open),
          .44f,
          k * .061f,
          .005f,
          0xffd8ddd2);
    }
    for (int k = 0; k < 6; k++) {
      float y = .277f + k * .033f;
      box(
          p,
          out,
          light,
          side < 0 ? -.103f : .075f,
          y,
          -.045f,
          side < 0 ? -.075f : .103f,
          y + .016f,
          .045f,
          0xff8b957e);
      box(
          p,
          out,
          light,
          side < 0 ? -.109f : .10f,
          y + .004f,
          -.025f,
          side < 0 ? -.10f : .109f,
          y + .009f,
          .025f,
          k % 2 == 0 ? 0xff97c6ba : 0xffd1b86e);
    }
    for (int wire = 0; wire < 5; wire++) {
      float lastX = side * .07f, lastY = .27f, lastZ = -.075f + wire * .035f;
      int color = new int[] {0xff913e37, 0xff3e6c72, 0xffb6a05b, 0xff233138, 0xff9eaaa4}[wire];
      for (int j = 1; j <= 7; j++) {
        float t = j / 7f,
            x = side * (.07f + .10f * open * t + .024f * (float) Math.sin(t * Math.PI));
        float y = .27f + .21f * t,
            z = -.075f + wire * .035f + .025f * (float) Math.sin(t * Math.PI);
        rod(p, out, light, lastX, lastY, lastZ, x, y, z, .0035f, color);
        lastX = x;
        lastY = y;
        lastZ = z;
      }
    }
    // Side-mounted hinge: its cylinder axis runs across the forearm, not through the fist.
    p.pushPose();
    p.translate(side * (width / 2 + .016 + .05 * open), .365, 0);
    cylinder(p, out, light, 0, 0, 0, .058f, .16f, 0xff8b7955);
    cylinder(p, out, light, 0, 0, 0, .034f, .177f, 0xff223139);
    cylinder(p, out, light, 0, 0, 0, .012f, .186f, 0xffc7cbbd);
    p.mulPose(Axis.XP.rotationDegrees(148 * (1 - m.blade()) - 30));
    // Fixed outer guide, with a visibly translating chrome inner guide; no geometry is scaled.
    for (int k = -1; k <= 1; k += 2) {
      box(p, out, light, k * .052f - .017f, 0, -.029f, k * .052f + .017f, .31f, .029f, 0xff3e4d54);
      box(
          p,
          out,
          light,
          k * .052f - .008f,
          .20f,
          -.013f,
          k * .052f + .008f,
          .35f + .30f * extension,
          .013f,
          0xffc0cbc6);
      box(
          p,
          out,
          light,
          k * .052f - .012f,
          .29f + .30f * extension,
          -.021f,
          k * .052f + .012f,
          .35f + .30f * extension,
          .021f,
          0xff7c8e90);
    }
    box(p, out, light, -.032f, .06f, -.039f, .032f, .235f, .039f, 0xffa2966d);
    for (int k = 0; k < 6; k++)
      box(
          p,
          out,
          light,
          -.035f,
          .08f + k * .025f,
          -.044f,
          .035f,
          .086f + k * .025f,
          -.038f,
          0xff26343b);
    rod(p, out, light, 0, .10f, .047f, 0, .37f + .30f * extension, .047f, .011f, 0xff3a484e);
    rod(p, out, light, 0, .20f, .047f, 0, .37f + .30f * extension, .047f, .005f, 0xffe0dfc9);
    p.translate(0, .35 + .30 * extension, 0);
    cylinder(p, out, light, 0, 0, 0, .041f, .16f, 0xff9eaa9f);
    box(p, out, light, -.061f, -.027f, -.034f, .061f, .075f, .034f, 0xff34434a);
    // Cutting assembly has its own hinge and a constant-size machined profile.
    p.mulPose(Axis.XP.rotationDegrees(-12 * m.blade()));
    blade(p, out, light, left);
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
          new float[] {mirror * a[0], a[1] * .80f, -.010f},
          new float[] {mirror * b[0], b[1] * .80f, -.010f},
          new float[] {mirror * b[0], b[1] * .80f, .010f},
          new float[] {mirror * a[0], a[1] * .80f, .010f},
          0xffedf3ef);
      for (int side : new int[] {-1, 1}) {
        float z = side * .010f;
        quad(
            p,
            out,
            light,
            new float[] {mirror * a[0], a[1] * .80f, z},
            new float[] {mirror * b[0], b[1] * .80f, z},
            new float[] {mirror * .025f, .49f, side * .032f},
            new float[] {mirror * .025f, .49f, side * .032f},
            side < 0 ? 0xffaebbbd : 0xff72838b);
      }
    }
    // Raised spine, recessed vent insets, long machined fuller and graduated edge etching.
    for (int face : new int[] {-1, 1}) {
      float z = face * .034f;
      box(p, out, light, -.019f, .12f, z - .002f, .021f, 1.02f, z + .002f, 0xff31434b);
      for (int i = 0; i < 9; i++) {
        float yy = .18f + i * .065f, xx = mirror * (.055f + .025f * (float) Math.sin(yy * 2));
        box(
            p,
            out,
            light,
            xx - .022f,
            yy,
            z - .003f,
            xx + .022f,
            yy + .028f,
            z + .003f,
            0xff25353c);
        box(p, out, light, xx - .02f, yy, z - .004f, xx + .02f, yy + .004f, z + .004f, 0xffbbc7c4);
      }
      for (int i = 0; i < 3; i++) {
        float yy = .09f + i * .055f;
        box(p, out, light, -.042f, yy, z - .005f, -.024f, yy + .015f, z + .005f, 0xffc1af78);
      }
      box(p, out, light, -.01f, .08f, z - .004f, .043f, .15f, z + .004f, 0xffa99561);
      for (int i = 0; i < 4; i++)
        box(
            p,
            out,
            light,
            .003f + i * .009f,
            .095f,
            z - .005f,
            .007f + i * .009f,
            .129f,
            z + .005f,
            0xff2c3d45);
    }
    for (int i = 0; i < 8; i++)
      box(
          p,
          out,
          light,
          -.064f,
          .17f + i * .036f,
          -.015f,
          -.047f,
          .185f + i * .036f,
          .015f,
          0xffcbd6cb);
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
