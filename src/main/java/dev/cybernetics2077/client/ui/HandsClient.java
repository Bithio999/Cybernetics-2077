package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.combat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** One local, bounded trajectory preview; no entities/particles are created for predicted shots. */
@EventBusSubscriber(modid = Cybernetics2077.ID, value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class HandsClient {
  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    dev.cybernetics2077.compat.TacZCompat.clientBlocked =
        entity ->
            entity == Minecraft.getInstance().player
                && (CyberwareClient.own().getBoolean("deployed")
                    || CyberwareClient.own().getBoolean("berserk")
                    || ScannerClient.active());
    dev.cybernetics2077.compat.TacZCompat.clientRecoil =
        entity -> {
          if (entity != Minecraft.getInstance().player) return 1;
          var own = CyberwareClient.own();
          return dev.cybernetics2077.compat.TacZCompat.recoil(
              own.getInt("shockTier"),
              own.getInt("immovableTier"),
              entity.onGround() && entity.getDeltaMovement().horizontalDistanceSqr() < .005);
        };
    HandsRuntime.clientImmovable =
        entity ->
            entity == Minecraft.getInstance().player
                ? CyberwareClient.own().getInt("immovableTier")
                : 0;
  }

  @SubscribeEvent
  public static void hud(RenderGuiEvent.Post e) {
    var m = Minecraft.getInstance();
    if (m.player == null || m.level == null || m.screen != null || m.options.hideGui) return;
    var t = CyberwareClient.own();
    if (t.getInt("smartTier") == 0
        || !m.player.isUsingItem()
            && !ScannerClient.active()
            && !dev.cybernetics2077.compat.TacZCompat.aiming(m.player)) return;
    var g = e.getGuiGraphics();
    float x = g.guiWidth() / 2f, y = g.guiHeight() / 2f + 26;
    CyberBatch.begin();
    try {
      var c = new CyberCanvas(g, 1);
      boolean locked = t.getInt("smartTarget") >= 0;
      c.panel(x - 91, y, 182, 18, 3, 0xc4112029, locked ? 0xff62dedb : 0xff6d8794);
      String text = locked ? "SMART LOCK: " + t.getString("smartName") : "SCAN AN ENEMY TO LOCK";
      c.center(c.fit(text, 174, 11, false), x, y + 4, 11, locked ? 0xff8fece4 : 0xffafbdc5);
    } finally {
      CyberBatch.flush(g);
    }
  }

  @SubscribeEvent
  public static void preview(RenderLevelStageEvent e) {
    var m = Minecraft.getInstance();
    if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
        || m.level == null
        || m.player == null
        || m.screen != null
        || !m.player.isUsingItem()
        || !(m.player.getUseItem().getItem() instanceof BowItem)) return;
    var data = CyberwareClient.own();
    int tier = data.getInt("ballisticTier");
    if (tier == 0) return;
    double power = BowItem.getPowerForTime(m.player.getTicksUsingItem());
    if (power < .1) return;
    Vec3 point = m.player.getEyePosition(),
        v = m.player.getLookAngle().scale(3 * power).add(m.player.getDeltaMovement());
    boolean bounced = false;
    var source = m.renderBuffers().bufferSource();
    var out = source.getBuffer(RenderType.lines());
    var pose = e.getPoseStack();
    pose.pushPose();
    var cam = e.getCamera().getPosition();
    pose.translate(-cam.x, -cam.y, -cam.z);
    for (int i = 0; i < 40; i++) {
      Vec3 next = point.add(v);
      var hit =
          m.level.clip(
              new ClipContext(
                  point, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, m.player));
      if (hit.getType() != HitResult.Type.MISS) {
        line(pose, out, point, hit.getLocation(), bounced ? 0xaaa9db70 : 0xaaffbd58);
        if (bounced) break;
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        var r = HandsRules.reflect(v.x, v.y, v.z, n.x, n.y, n.z);
        v = new Vec3(r.x(), r.y(), r.z()).scale(HandsRules.bounceSpeed(tier));
        point = hit.getLocation().add(n.scale(.06));
        bounced = true;
      } else {
        line(pose, out, point, point.lerp(next, .55), bounced ? 0xaaa9db70 : 0xaaffbd58);
        point = next;
      }
      v =
          v.scale(.99)
              .add(
                  0,
                  -.05
                      * (1
                          - (data.getInt("immovableTier") > 0
                              ? .2 + .05 * data.getInt("immovableTier")
                              : 0)),
                  0);
    }
    pose.popPose();
    source.endBatch(RenderType.lines());
  }

  private static void line(PoseStack pose, VertexConsumer out, Vec3 a, Vec3 b, int color) {
    Vec3 n = b.subtract(a).normalize();
    out.addVertex(pose.last(), (float) a.x, (float) a.y, (float) a.z)
        .setColor(color)
        .setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
    out.addVertex(pose.last(), (float) b.x, (float) b.y, (float) b.z)
        .setColor(color)
        .setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = net.neoforged.api.distmarker.Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
      event.enqueueWork(
          () ->
              CyberwareNetwork.trajectory =
                  packet -> {
                    var m = Minecraft.getInstance();
                    if (m.level == null) return;
                    var entity = m.level.getEntity(packet.entity());
                    if (entity == null
                        || !entity.getUUID().equals(packet.uuid())
                        || !dev.cybernetics2077.compat.TacZCompat.bullet(entity)) return;
                    entity.setPos(packet.x(), packet.y(), packet.z());
                    entity.setDeltaMovement(packet.vx(), packet.vy(), packet.vz());
                  });
    }
  }

  private HandsClient() {}
}
