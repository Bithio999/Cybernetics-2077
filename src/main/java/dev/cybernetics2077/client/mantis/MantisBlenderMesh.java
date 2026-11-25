package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import java.io.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Shared atlas and cached mesh arrays, with a much lighter model beyond sixteen blocks. */
public final class MantisBlenderMesh {
  public static final net.minecraft.resources.ResourceLocation ATLAS =
      Cybernetics2077.id("textures/entity/mantis_blender.png");

  public static net.minecraft.resources.ResourceLocation atlas(
      dev.cybernetics2077.cyberware.MantisVariant variant) {
    return variant == dev.cybernetics2077.cyberware.MantisVariant.PHYSICAL
        ? ATLAS
        : Cybernetics2077.id("textures/entity/mantis_" + variant.id + ".png");
  }

  private static final java.util.Map<dev.cybernetics2077.cyberware.MantisVariant, MantisMeshData[]>
      MODELS = new java.util.EnumMap<>(dev.cybernetics2077.cyberware.MantisVariant.class);

  private static MantisMeshData load(
      net.minecraft.server.packs.resources.ResourceManager manager, String name) {
    try (var stream =
        manager
            .getResourceOrThrow(Cybernetics2077.id("meshes/mantis/" + name + ".cymesh"))
            .open()) {
      return MantisMeshData.read(stream);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load Blender Mantis mesh: " + name, e);
    }
  }

  public static void reload() {
    MODELS.clear();
  }

  public static void preload(net.minecraft.server.packs.resources.ResourceManager manager) {
    MODELS.clear();
    for (var variant : dev.cybernetics2077.cyberware.MantisVariant.values()) {
      String prefix =
          variant == dev.cybernetics2077.cyberware.MantisVariant.PHYSICAL ? "" : variant.id + "_";
      MODELS.put(
          variant,
          new MantisMeshData[] {load(manager, prefix + "near"), load(manager, prefix + "far")});
    }
  }

  public static MantisMeshData model(boolean distant) {
    return model(dev.cybernetics2077.cyberware.MantisVariant.PHYSICAL, distant);
  }

  public static MantisMeshData model(
      dev.cybernetics2077.cyberware.MantisVariant variant, boolean distant) {
    if (!MODELS.containsKey(variant)) preload(Minecraft.getInstance().getResourceManager());
    return MODELS.get(variant)[distant ? 1 : 0];
  }

  private static void part(
      PoseStack p, VertexConsumer out, int light, MantisMeshData model, String name) {
    float[] v = model.parts().get(name).vertices();
    for (int i = 0; i < v.length; i += 8)
      out.addVertex(p.last(), v[i], v[i + 1], v[i + 2])
          .setColor(-1)
          .setUv(v[i + 3], v[i + 4])
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(light)
          .setNormal(p.last(), v[i + 5], v[i + 6], v[i + 7]);
  }

  public static void render(
      PoseStack p,
      VertexConsumer out,
      int light,
      MantisMotion.Pose motion,
      boolean distant,
      float depth,
      dev.cybernetics2077.cyberware.MantisVariant variant) {
    var model = model(variant, distant);
    p.pushPose();
    p.mulPose(MantisArticulation.mount(motion.blade(), depth));
    part(p, out, light, model, "mount");
    p.mulPose(MantisArticulation.carriage(motion.slide()));
    part(p, out, light, model, "carriage");
    p.mulPose(MantisArticulation.support(motion.blade(), motion.extension()));
    part(p, out, light, model, "support");
    p.mulPose(MantisArticulation.blade(motion.blade(), motion.extension()));
    part(p, out, light, model, "blade");
    p.mulPose(MantisArticulation.tip(motion.tip()));
    part(p, out, light, model, "tip");
    p.mulPose(MantisArticulation.edge(motion.tip()));
    part(p, out, light, model, "edge");
    p.popPose();
  }

  private MantisBlenderMesh() {}
}
