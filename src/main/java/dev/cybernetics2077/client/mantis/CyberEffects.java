package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.ui.ScannerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Target-mounted upload progress and short, deterministic electrical filaments. */
@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class CyberEffects {
  @SubscribeEvent
  public static void render(RenderLevelStageEvent e) {
    if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    var m = Minecraft.getInstance();
    if (m.level == null || m.player == null) return;
    var camera = e.getCamera();
    var cameraPos = camera.getPosition();
    var p = e.getPoseStack();
    var buffers = m.renderBuffers().bufferSource();
    float partial = e.getPartialTick().getGameTimeDeltaPartialTick(false);
    double now = m.level.getGameTime() + (double) partial;
    if (ScannerClient.active()) {
      var queue = CyberwareClient.own().getList("uploads", 10);
      java.util.Map<Integer, Integer> counts = new java.util.HashMap<>();
      for (int i = 0; i < queue.size(); i++) {
        var q = queue.getCompound(i);
        int id = q.getInt("target");
        var entity = m.level.getEntity(id);
        if (entity == null) continue;
        int row = counts.merge(id, 1, Integer::sum) - 1;
        var position =
            entity
                .getPosition(partial)
                .add(0, entity.getBbHeight() + .55 + row * .35, 0)
                .subtract(cameraPos);
        p.pushPose();
        p.translate(position.x, position.y, position.z);
        p.mulPose(camera.rotation());
        // Match Minecraft 1.21 nameplate handedness so text glyphs face the camera.
        p.scale(.012f, -.012f, .012f);
        var out =
            buffers.getBuffer(
                RenderType.entityTranslucent(
                    Cybernetics2077.id("textures/entity/effect_white.png")));
        var label =
            Component.literal(now < q.getLong("start") ? "QUEUED" : "SHORT CIRCUIT")
                .withStyle(
                    net.minecraft.network.chat.Style.EMPTY.withFont(Cybernetics2077.id("ui")));
        float textWidth = m.font.width(label), half = Math.max(45, textWidth / 2 + 6);
        rect(p, out, -half, -9, half, 8, 0xe9121823);
        float progress = (float) Math.clamp((now - q.getLong("start")) / 20, 0, 1);
        p.translate(0, 0, .02f);
        rect(p, out, -half + 1, 6, half - 1, 9, 0xff304651);
        p.translate(0, 0, .02f);
        rect(p, out, -half + 1, 6, -half + 1 + (half * 2 - 2) * progress, 9, 0xff63e4f7);
        p.translate(0, 0, .02f);
        m.font.drawInBatch(
            label,
            -textWidth / 2,
            -5,
            0xff8eeafa,
            false,
            p.last().pose(),
            buffers,
            net.minecraft.client.gui.Font.DisplayMode.NORMAL,
            0,
            0xf000f0);
        p.popPose();
      }
    }
    for (var entry : CyberwareClient.IMPACTS.entrySet()) {
      var entity = m.level.getEntity(entry.getKey());
      if (entity == null) continue;
      float age = (float) (now - entry.getValue());
      float alpha = 1 - age / 14;
      if (alpha <= 0) continue;
      var origin = entity.getPosition(partial).subtract(cameraPos);
      p.pushPose();
      p.translate(origin.x, origin.y, origin.z);
      var out =
          buffers.getBuffer(
              RenderType.entityTranslucent(Cybernetics2077.id("textures/entity/effect_white.png")));
      for (int strand = 0; strand < 5; strand++) {
        var previous = new Vec3(0, entity.getBbHeight() * .5, 0);
        for (int k = 1; k <= 8; k++) {
          double a = strand * 1.256 + k * 1.77 + Math.floor(age * 1.5);
          var next =
              new Vec3(
                  Math.cos(a) * entity.getBbWidth() * .6,
                  (k / 8.0) * entity.getBbHeight(),
                  Math.sin(a) * entity.getBbWidth() * .6);
          line(p, out, previous, next, .012f, (Math.round(alpha * 220) << 24) | 0x85efff);
          previous = next;
        }
      }
      p.popPose();
    }
    buffers.endBatch();
  }

  private static void rect(
      PoseStack p, VertexConsumer out, float x, float y, float X, float Y, int color) {
    var m = p.last();
    for (float[] v : new float[][] {{x, y, 0, 0}, {x, Y, 0, 1}, {X, Y, 1, 1}, {X, y, 1, 0}})
      out.addVertex(m, v[0], v[1], .01f)
          .setColor(color)
          .setUv(v[2], v[3])
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(0xf000f0)
          .setNormal(m, 0, 0, 1);
  }

  private static void line(
      PoseStack p, VertexConsumer out, Vec3 a, Vec3 b, float width, int color) {
    var side =
        b.subtract(a)
            .cross(
                new Vec3(
                    Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector().x(),
                    Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector().y(),
                    Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector().z()))
            .normalize()
            .scale(width);
    var m = p.last();
    var vertices = new Vec3[] {a.subtract(side), a.add(side), b.add(side), b.subtract(side)};
    for (int i = 0; i < 4; i++)
      out.addVertex(m, (float) vertices[i].x, (float) vertices[i].y, (float) vertices[i].z)
          .setColor(color)
          .setUv(i < 2 ? 0 : 1, i % 3 == 0 ? 0 : 1)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(0xf000f0)
          .setNormal(m, 0, 1, 0);
  }

  private CyberEffects() {}
}
