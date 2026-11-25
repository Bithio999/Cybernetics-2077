package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
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
  private static final ByteBufferBuilder SKIN_MEMORY = new ByteBufferBuilder(65536),
      METAL_MEMORY = new ByteBufferBuilder(262144),
      FALLBACK_MEMORY = new ByteBufferBuilder(256),
      MODEL_MEMORY = new ByteBufferBuilder(1024 * 1024),
      EFFECT_MEMORY = new ByteBufferBuilder(16384);
  private static MultiBufferSource.BufferSource isolated;
  private static net.minecraft.resources.ResourceLocation currentSkin;
  private static dev.cybernetics2077.cyberware.MantisVariant currentVariant;

  private static MultiBufferSource.BufferSource source(
      net.minecraft.resources.ResourceLocation skin,
      dev.cybernetics2077.cyberware.MantisVariant variant) {
    if (isolated == null || !skin.equals(currentSkin) || variant != currentVariant) {
      var fixed = new java.util.LinkedHashMap<RenderType, ByteBufferBuilder>();
      fixed.put(RenderType.entityCutoutNoCull(skin), SKIN_MEMORY);
      fixed.put(RenderType.entityCutoutNoCull(METAL), METAL_MEMORY);
      fixed.put(MantisMaterial.type(variant), MODEL_MEMORY);
      fixed.put(MantisElementalEffects.TYPE, EFFECT_MEMORY);
      isolated = MultiBufferSource.immediateWithBuffers(fixed, FALLBACK_MEMORY);
      currentSkin = skin;
      currentVariant = variant;
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
    float depth = slim ? 3 / 16f : 4 / 16f;
    boolean capture = dev.cybernetics2077.client.temporal.SandevistanTrails.capturing;
    var variant =
        dev.cybernetics2077.cyberware.MantisVariant.of(
            CyberwareClient.state(player.getId()).getInt("bladeVariant"));
    MultiBufferSource local = capture ? buffers : source(player.getSkin().texture(), variant);
    var skin = local.getBuffer(RenderType.entityCutoutNoCull(player.getSkin().texture()));
    var metal = local.getBuffer(RenderType.entityCutoutNoCull(METAL));
    var modelBuffer = local.getBuffer(MantisMaterial.type(variant));
    var sleeveFlag =
        left
            ? net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE
            : net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE;
    boolean sleeves = player.isModelPartShown(sleeveFlag);
    section(p, skin, metal, light, player, left, slim, depth, motion, sleeves, true);
    p.pushPose();
    p.mulPose(MantisRig.elbow(motion));
    section(p, skin, metal, light, player, left, slim, depth, motion, sleeves, false);
    if (motion.panels() > .001f) {
      boolean distant =
          capture
              || !firstPerson
                  && net.minecraft.client.Minecraft.getInstance()
                          .gameRenderer
                          .getMainCamera()
                          .getPosition()
                          .distanceToSqr(player.position())
                      > 256;
      MantisBlenderMesh.render(p, modelBuffer, light, motion, distant, depth, variant);
      // Omit effects from captured Sandevistan poses and the distant LOD. The live blades
      // stay readable without duplicating vapor/arcs across every afterimage.
      if (!capture && !distant) {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        double time =
            player.tickCount
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false)
                + player.getId() * 17.0
                + (left ? 23 : 0);
        float visibility =
            firstPerson
                ? 1
                : (float)
                    Math.clamp(
                        (16
                                - minecraft
                                    .gameRenderer
                                    .getMainCamera()
                                    .getPosition()
                                    .distanceTo(player.position()))
                            / 6,
                        0,
                        1);
        MantisElementalEffects.render(
            p,
            local.getBuffer(MantisElementalEffects.TYPE),
            motion,
            depth,
            variant,
            time,
            visibility);
      }
    }
    p.popPose();
    if (!capture) ((MultiBufferSource.BufferSource) local).endBatch();
  }

  private static void section(
      PoseStack p,
      VertexConsumer skin,
      VertexConsumer metal,
      int light,
      AbstractClientPlayer player,
      boolean left,
      boolean slim,
      float depth,
      MantisMotion.Pose motion,
      boolean sleeves,
      boolean upper) {
    float width = MantisRig.ARM_WIDTH;
    float from = upper ? -1 : MantisRig.ELBOW_Y, to = upper ? MantisRig.ELBOW_Y : 1;
    float border = width * MantisSkinPanels.WIDTH;
    if (motion.panels() > .001f) {
      // Solid subdermal tissue remains below the exposed harness. It ends before
      // the hand, and has no rectangular metal door frame or hollow arm tube.
      box(
          p,
          metal,
          light,
          -width / 2 + .006f,
          upper ? MantisSkinPanels.START : MantisRig.ELBOW_Y,
          -depth / 2 + .060f,
          width / 2 - .006f,
          upper ? MantisRig.ELBOW_Y : MantisSkinPanels.END,
          depth / 2 - .007f,
          0xff463438);
    }
    for (int layer = 0; layer < 2; layer++) {
      if (layer == 1 && !sleeves) continue;
      var faces = NativeArmSkin.mesh(player, left, layer == 1, slim);
      for (var face : faces) {
        var normal = face.getFirst();
        if (normal.nz() < -.5f) {
          NativeArmSkin.part(p, skin, light, face, -1, -border, from, to);
          NativeArmSkin.part(p, skin, light, face, border, 1, from, to);
          if (upper) {
            NativeArmSkin.part(p, skin, light, face, -border, border, from, MantisSkinPanels.START);
            panel(
                p, skin, metal, light, face, MantisSkinPanels.Panel.PROXIMAL, depth, motion, layer);
          } else {
            NativeArmSkin.part(p, skin, light, face, -border, border, MantisSkinPanels.END, to);
            for (var part : MantisSkinPanels.ALL)
              if (part != MantisSkinPanels.Panel.PROXIMAL
                  && part != MantisSkinPanels.Panel.SIDE_LEFT
                  && part != MantisSkinPanels.Panel.SIDE_RIGHT)
                panel(p, skin, metal, light, face, part, depth, motion, layer);
          }
        } else if (!upper && Math.abs(normal.nx()) > .5f) {
          NativeArmSkin.part(p, skin, light, face, -1, 1, from, to, -.01f, 1);
          NativeArmSkin.part(p, skin, light, face, -1, 1, MantisSkinPanels.SIDE_END, to, -1, -.01f);
          panel(
              p,
              skin,
              metal,
              light,
              face,
              normal.nx() < 0
                  ? MantisSkinPanels.Panel.SIDE_LEFT
                  : MantisSkinPanels.Panel.SIDE_RIGHT,
              depth,
              motion,
              layer);
        } else NativeArmSkin.part(p, skin, light, face, -1, 1, from, to);
      }
    }
  }

  private static void panel(
      PoseStack p,
      VertexConsumer skin,
      VertexConsumer metal,
      int light,
      java.util.List<NativeArmSkin.V> face,
      MantisSkinPanels.Panel part,
      float depth,
      MantisMotion.Pose motion,
      int layer) {
    float width = MantisRig.ARM_WIDTH, surface = -depth / 2;
    var transform = MantisSkinPanels.transform(part, depth, motion.panels(), layer == 1);
    if (layer == 0 && motion.panels() > .001f) {
      float side =
          (part == MantisSkinPanels.Panel.MID_LEFT
                  || part == MantisSkinPanels.Panel.CARRIER_LEFT
                  || part == MantisSkinPanels.Panel.SIDE_LEFT)
              ? -1
              : 1;
      float border = width * MantisSkinPanels.WIDTH;
      switch (part) {
        case PROXIMAL -> {
          for (int sign : new int[] {-1, 1}) {
            var tip =
                transform.transformPosition(new Vector3f(sign * width * .18f, .015f, surface));
            strut(p, metal, light, new Vector3f(sign * width * .18f, -.025f, surface - .005f), tip);
          }
        }
        case WRIST -> {
          for (int sign : new int[] {-1, 1}) {
            var tip = transform.transformPosition(new Vector3f(sign * width * .18f, .50f, surface));
            strut(p, metal, light, new Vector3f(sign * width * .18f, .515f, surface - .005f), tip);
          }
        }
        case MID_LEFT, MID_RIGHT, CARRIER_LEFT, CARRIER_RIGHT -> {
          float y =
              (part == MantisSkinPanels.Panel.MID_LEFT || part == MantisSkinPanels.Panel.MID_RIGHT)
                  ? .20f
                  : .415f;
          var tip = transform.transformPosition(new Vector3f(side * (border - .008f), y, surface));
          strut(p, metal, light, new Vector3f(side * (border - .014f), y, surface + .035f), tip);
        }
        case SIDE_LEFT, SIDE_RIGHT -> {
          var tip =
              transform.transformPosition(new Vector3f(side * width / 2, .32f, surface + .045f));
          strut(
              p, metal, light, new Vector3f(side * (width / 2 - .01f), .32f, surface + .055f), tip);
        }
      }
    }
    p.pushPose();
    p.mulPose(transform);
    NativeArmSkin.panel(
        p, skin, metal, light, face, MantisSkinPanels.region(part, depth), layer == 0);
    p.popPose();
  }

  /** Small telescoping connections keep the moved dermal covers visibly attached. */
  private static void strut(PoseStack p, VertexConsumer out, int light, Vector3f a, Vector3f b) {
    var direction = new Vector3f(b).sub(a);
    float length = direction.length();
    if (length < .004f) return;
    direction.div(length);
    p.pushPose();
    p.translate(a.x, a.y, a.z);
    p.mulPose(
        new org.joml.Quaternionf().rotationTo(0, 1, 0, direction.x, direction.y, direction.z));
    box(p, out, light, -.006f, 0, -.006f, .006f, length * .65f, .006f, 0xff344148);
    box(p, out, light, -.0035f, length * .50f, -.0035f, .0035f, length, .0035f, 0xff9ba9aa);
    p.popPose();
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
