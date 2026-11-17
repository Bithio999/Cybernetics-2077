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
  private static MantisMeshData near, far;

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
    near = null;
    far = null;
  }

  public static void preload(net.minecraft.server.packs.resources.ResourceManager manager) {
    var newNear = load(manager, "near");
    var newFar = load(manager, "far");
    near = newNear;
    far = newFar;
  }

  public static MantisMeshData model(boolean distant) {
    if (distant) {
      if (far == null) far = load(Minecraft.getInstance().getResourceManager(), "far");
      return far;
    }
    if (near == null) near = load(Minecraft.getInstance().getResourceManager(), "near");
    return near;
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
      PoseStack p, VertexConsumer out, int light, MantisMotion.Pose motion, boolean distant) {
    var model = model(distant);
    p.pushPose();
    p.mulPose(MantisArticulation.mount(motion.panels()));
    part(p, out, light, model, "mount");
    p.mulPose(MantisArticulation.support(motion.blade()));
    part(p, out, light, model, "support");
    p.mulPose(MantisArticulation.blade(motion.blade(), motion.extension()));
    part(p, out, light, model, "blade");
    p.mulPose(MantisArticulation.tip(motion.tip()));
    part(p, out, light, model, "tip");
    p.popPose();
  }

  private MantisBlenderMesh() {}
}
