package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import org.joml.Vector3f;

/** Blender blades and articulated forearm shells, with the wearer's real skin UVs. */
public final class MantisMesh {
  private static final net.minecraft.resources.ResourceLocation METAL =
      Cybernetics2077.id("textures/entity/mantis_mechanism.png");
  private static final ByteBufferBuilder SKIN_MEMORY = new ByteBufferBuilder(16384),
      METAL_MEMORY = new ByteBufferBuilder(262144),
      FALLBACK_MEMORY = new ByteBufferBuilder(256),
      MODEL_MEMORY = new ByteBufferBuilder(1024 * 1024);
  private static MultiBufferSource.BufferSource isolated;
  private static net.minecraft.resources.ResourceLocation currentSkin;

  private static MultiBufferSource.BufferSource source(
      net.minecraft.resources.ResourceLocation skin) {
    if (isolated == null || !skin.equals(currentSkin)) {
      var fixed = new java.util.LinkedHashMap<RenderType, ByteBufferBuilder>();
      fixed.put(RenderType.entityCutoutNoCull(skin), SKIN_MEMORY);
      fixed.put(RenderType.entityCutoutNoCull(METAL), METAL_MEMORY);
      fixed.put(MantisMaterial.TYPE, MODEL_MEMORY);
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
    var modelBuffer = local.getBuffer(MantisMaterial.TYPE);
    var sleeveFlag =
        left
            ? net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE
            : net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE;
    float window = width * .44f;
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
          MantisRig.ELBOW_Y - .015f,
          -.123f,
          width / 2 - .002f,
          MantisRig.ELBOW_Y,
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
          NativeArmSkin.part(p, skin, light, face, -1, 1, MantisRig.ELBOW_Y, 1);
          continue;
        }
        // Four strips retain the original arm silhouette around the hatch.
        NativeArmSkin.part(p, skin, light, face, -1, -window, MantisRig.ELBOW_Y, 1);
        NativeArmSkin.part(p, skin, light, face, window, 1, MantisRig.ELBOW_Y, 1);
        NativeArmSkin.part(p, skin, light, face, -window, window, MantisRig.ELBOW_Y, .11f);
        NativeArmSkin.part(p, skin, light, face, -window, window, .52f, 1);
        for (int flap : new int[] {-1, 1}) {
          float edge = flap * window;
          p.pushPose();
          p.translate(edge + flap * .018f * motion.panels(), 0, z - .018f * motion.panels());
          p.mulPose(Axis.YP.rotationDegrees(-flap * motion.panels() * 105));
          p.translate(-edge, 0, -z);
          NativeArmSkin.part(
              p, skin, light, face, flap < 0 ? -window : 0, flap < 0 ? 0 : window, .11f, .52f);
          if (layer == 0 && motion.panels() > .001f)
            box(
                p,
                metal,
                light,
                flap < 0 ? -window + .002f : .002f,
                .113f,
                z + .0015f,
                flap < 0 ? -.002f : window - .002f,
                .517f,
                z + .004f,
                0xff3a484e);
          p.popPose();
        }
      }
    }
    if (motion.panels() > .001f) {
      sealedRecess(p, metal, light, width);
      boolean distant =
          !firstPerson
              && net.minecraft.client.Minecraft.getInstance()
                      .gameRenderer
                      .getMainCamera()
                      .getPosition()
                      .distanceToSqr(player.position())
                  > 256;
      MantisBlenderMesh.render(p, modelBuffer, light, motion, distant);
    }
    p.popPose();
    local.endBatch();
  }

  /** Backing stays opaque independently of model detail level. */
  private static void sealedRecess(PoseStack p, VertexConsumer out, int light, float width) {
    float w = width * .44f;
    box(
        p,
        out,
        light,
        -width / 2 + .002f,
        MantisRig.ELBOW_Y,
        -.075f,
        width / 2 - .002f,
        .52f,
        .123f,
        0xff293840);
    box(p, out, light, -w - .005f, .11f, -.123f, -w + .005f, .52f, -.075f, 0xff677d81);
    box(p, out, light, w - .005f, .11f, -.123f, w + .005f, .52f, -.075f, 0xff677d81);
    box(p, out, light, -w, .515f, -.123f, w, .53f, -.075f, 0xff677d81);
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
