package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.MantisVariant;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Small emissive ribbons attached to the real moving receivers, never world particle entities. */
public final class MantisElementalEffects {
  static final RenderType TYPE =
      RenderType.entityTranslucentEmissive(
          Cybernetics2077.id("textures/entity/mantis_elemental.png"));
  public static int lastQuads;

  static void render(
      PoseStack p,
      VertexConsumer out,
      MantisMotion.Pose motion,
      float depth,
      MantisVariant variant,
      double clock,
      float visibility) {
    lastQuads = 0;
    if (variant == MantisVariant.PHYSICAL || visibility <= 0 || motion.blade() < .94f) return;
    float fade = Math.clamp((motion.blade() - .94f) / .06f, 0, 1) * visibility;
    p.pushPose();
    p.mulPose(MantisArticulation.mount(motion.blade(), depth));
    p.mulPose(MantisArticulation.carriage(motion.slide()));
    p.mulPose(MantisArticulation.support(motion.blade(), motion.extension()));
    p.mulPose(MantisArticulation.blade(motion.blade(), motion.extension()));
    receiver(p, out, variant, clock, .39f, -.060f, fade);
    p.mulPose(MantisArticulation.tip(motion.tip()));
    receiver(p, out, variant, clock + 13, .30f, -.034f, fade * .70f);
    p.mulPose(MantisArticulation.edge(motion.tip()));
    receiver(p, out, variant, clock + 29, .30f, -.047f, fade * .60f);
    p.popPose();
  }

  private static int color(int rgb, double alpha) {
    return (Math.clamp((int) alpha, 0, 255) << 24) | rgb;
  }

  private static void receiver(
      PoseStack p,
      VertexConsumer out,
      MantisVariant v,
      double clock,
      float length,
      float spine,
      float fade) {
    float time = (float) clock;
    switch (v) {
      case THERMAL -> {
        ribbon(p, out, 0, .02f, length, spine, spine, .009f, color(0xff9b46, 125 * fade));
        for (int i = 0; i < 4; i++) {
          float phase = (time * .017f + i * .25f) % 1;
          float y = .025f + length * (i + .5f) / 4;
          float rise = .012f + phase * .030f;
          float drift = (float) Math.sin(time * .10 + i * 2.1) * .007f;
          int c = color(0xff9a4e, 65 * Math.sin(phase * Math.PI) * fade);
          ribbon(p, out, drift, y, y + .022f, spine - rise, spine - rise - .018f, .012f, c);
          ribbon(
              p, out, -drift, y + .006f, y + .030f, spine - rise, spine - rise - .025f, .003f, c);
        }
      }
      case ELECTRICAL -> {
        ribbon(p, out, 0, .02f, length, spine, spine, .004f, color(0x5dcfff, 90 * fade));
        // Continuous time interpolation makes filaments crawl rather than flash whole blades.
        for (int strand = 0; strand < 2; strand++) {
          float oldY = .035f, oldZ = spine;
          for (int j = 1; j <= 5; j++) {
            float y = .035f + (length - .045f) * j / 5;
            float z = spine + (float) Math.sin(time * .32 + j * 3.2 + strand * 2.7) * .012f;
            float pulse =
                .45f + .55f * (float) Math.pow(.5 + .5 * Math.sin(time * .12 + strand * 2.7), 3);
            ribbon(
                p,
                out,
                (strand == 0 ? -1 : 1) * .020f,
                oldY,
                y,
                oldZ,
                z,
                .0032f,
                color(0x9defff, 175 * pulse * fade));
            oldY = y;
            oldZ = z;
          }
        }
      }
      case TOXIC -> {
        ribbon(p, out, 0, .025f, length, spine, spine, .006f, color(0x9ed64b, 80 * fade));
        for (int i = 0; i < 3; i++) {
          float phase = (time * .009f + i / 3f) % 1;
          float y = .025f + (length - .03f) * (i + .5f) / 3;
          float drift = (float) Math.sin(time * .045 + i * 2.3) * .010f;
          float z = spine - .010f - phase * .045f;
          int c = color(0xb5dc70, 48 * Math.sin(phase * Math.PI) * fade);
          ribbon(p, out, drift, y, y + .028f, z, z - .015f, .010f, c);
          ribbon(p, out, drift * .5f, y + .020f, y + .043f, z - .012f, z - .026f, .006f, c);
        }
      }
      default -> {}
    }
  }

  private static void ribbon(
      PoseStack p,
      VertexConsumer out,
      float x,
      float y0,
      float y1,
      float z0,
      float z1,
      float width,
      int color) {
    // Crossed planes keep a narrow filament readable from both the front and side.
    vertex(p, out, x - width, y0, z0, 0, 0, color);
    vertex(p, out, x - width, y1, z1, 1, 0, color);
    vertex(p, out, x + width, y1, z1, 1, 1, color);
    vertex(p, out, x + width, y0, z0, 0, 1, color);
    vertex(p, out, x, y0, z0 - width, 0, 0, color);
    vertex(p, out, x, y1, z1 - width, 1, 0, color);
    vertex(p, out, x, y1, z1 + width, 1, 1, color);
    vertex(p, out, x, y0, z0 + width, 0, 1, color);
    lastQuads += 2;
  }

  private static void vertex(
      PoseStack p, VertexConsumer out, float x, float y, float z, float u, float v, int color) {
    out.addVertex(p.last(), x, y, z)
        .setColor(color)
        .setUv(u, v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(0xf000f0)
        .setNormal(p.last(), 0, 0, 1);
  }

  private MantisElementalEffects() {}
}
