package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.compat.TacZCompat;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

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

  private static Matrix4f view, projection;
  private static Vec3 camera;
  private static Object viewLevel;

  @SubscribeEvent
  public static void hud(RenderGuiEvent.Post e) {
    var m = Minecraft.getInstance();
    if (m.player == null
        || m.level == null
        || m.screen != null
        || m.options.hideGui
        || ScannerClient.active()) return;
    var t = CyberwareClient.own();
    if (t.getInt("smartTier") == 0
        || !m.player.isUsingItem() && !TacZCompat.gun(m.player.getMainHandItem())) return;
    var g = e.getGuiGraphics();
    CyberBatch.begin();
    try {
      var c = new CyberCanvas(g, 1);
      var target = m.level.getEntity(t.getInt("smartTarget"));
      if (target == null || !target.isAlive()) {
        c.center("SCAN TO LINK", g.guiWidth() / 2f, g.guiHeight() / 2f + 24, 8, 0xff8ca5ac);
        return;
      }
      if (viewLevel != m.level || view == null || projection == null || camera == null) return;
      float partial = m.getTimer().getGameTimeDeltaPartialTick(false);
      Vec3 center =
          target.getPosition(partial).add(0, target.getBbHeight() * .55, 0).subtract(camera);
      var clip =
          new Vector4f((float) center.x, (float) center.y, (float) center.z, 1)
              .mul(view)
              .mul(projection);
      if (clip.w <= 0 || Math.abs(clip.x) > clip.w || Math.abs(clip.y) > clip.w) return;
      float x = (clip.x / clip.w + 1) * g.guiWidth() / 2,
          y = (1 - clip.y / clip.w) * g.guiHeight() / 2;
      float size =
          (float)
              Math.clamp(
                  g.guiHeight()
                      * .38
                      * target.getBbHeight()
                      / Math.max(1, m.player.distanceTo(target)),
                  12,
                  44);
      int cyan = 0xff70e6dc;
      for (int sx : new int[] {-1, 1})
        for (int sy : new int[] {-1, 1}) {
          float a = x + sx * size, b = y + sy * size;
          c.line(a, b, a - sx * 5, b, 1, cyan);
          c.line(a, b, a, b - sy * 5, 1, cyan);
        }
      c.rect(x - 1, y - 1, 2, 2, cyan);
      float labelY = Math.clamp(y + size + 5, 10, g.guiHeight() - 23);
      c.center(
          c.fit(t.getString("smartName").toUpperCase(Locale.ROOT), 130, 8, false),
          x,
          labelY,
          8,
          cyan);
      c.center(
          "LINKED / " + Math.round(m.player.distanceTo(target)) + " M",
          x,
          labelY + 10,
          7,
          0xffa4babe);
      float remaining =
          (float)
              Math.clamp(
                  (t.getLong("smartUntil") - m.level.getGameTime())
                      / (20.0 * (2 + t.getInt("smartTier"))),
                  0,
                  1);
      c.rect(x - 18, labelY + 19, 36, 1, 0x663e6970);
      c.rect(x - 18, labelY + 19, 36 * remaining, 1, cyan);
    } finally {
      CyberBatch.flush(g);
    }
  }

  public record Segment(Vec3 from, Vec3 to, boolean bounced) {}

  /** The centre trajectory, not a second shot. Uses at most 96 segments and 160 blocks. */
  public static List<Segment> prediction() {
    var m = Minecraft.getInstance();
    if (m.level == null || m.player == null || m.screen != null) return List.of();
    var data = CyberwareClient.own();
    int tier = data.getInt("ballisticTier");
    if (tier == 0) return List.of();
    double speed, gravity, friction;
    int life;
    if (m.player.isUsingItem() && m.player.getUseItem().getItem() instanceof BowItem) {
      double power = BowItem.getPowerForTime(m.player.getTicksUsingItem());
      if (power < .1) return List.of();
      speed = 3 * power;
      gravity = .05 * (1 - HandsRules.gravityReduction(data.getInt("immovableTier")));
      friction = .01;
      life = 80;
    } else {
      if (!TacZCompat.aiming(m.player)) return List.of();
      var profile = TacZCompat.ballistics(m.player);
      if (profile == null) return List.of();
      speed = profile.speed();
      gravity = profile.gravity();
      friction = profile.friction();
      life = profile.lifeTicks();
    }
    Vec3 point = m.player.getEyePosition(), v = m.player.getLookAngle().scale(speed);
    if (!TacZCompat.gun(m.player.getMainHandItem())) v = v.add(m.player.getDeltaMovement());
    boolean bounced = false;
    double distance = 0;
    var segments = new ArrayList<Segment>();
    for (int i = 0; i < Math.min(96, life) && distance < 160 && v.lengthSqr() > .0001; i++) {
      Vec3 next = point.add(v.scale(Math.min(1, (160 - distance) / v.length())));
      var hit =
          TacZCompat.gun(m.player.getMainHandItem())
              ? TacZCompat.clip(m.level, point, next, m.player)
              : m.level.clip(
                  new ClipContext(
                      point, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, m.player));
      Vec3 end = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation();
      var entity =
          net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
              m.player,
              point,
              end,
              new AABB(point, end).inflate(.3),
              x -> x.isAlive() && x.isPickable() && !x.isSpectator(),
              point.distanceToSqr(end));
      if (entity != null) {
        segments.add(new Segment(point, entity.getLocation(), bounced));
        break;
      }
      segments.add(new Segment(point, end, bounced));
      distance += point.distanceTo(end);
      if (hit.getType() != HitResult.Type.MISS) {
        if (bounced) break;
        Vec3 n = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        var r = HandsRules.reflect(v.x, v.y, v.z, n.x, n.y, n.z);
        v = new Vec3(r.x(), r.y(), r.z()).scale(HandsRules.bounceSpeed(tier));
        point = end.add(n.scale(.08));
        bounced = true;
      } else {
        point = next;
        v = v.scale(1 - friction).add(0, -gravity, 0);
      }
    }
    return segments;
  }

  @SubscribeEvent
  public static void preview(RenderLevelStageEvent e) {
    if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
    var m = Minecraft.getInstance();
    view = new Matrix4f(e.getModelViewMatrix());
    projection = new Matrix4f(e.getProjectionMatrix());
    camera = e.getCamera().getPosition();
    viewLevel = m.level;
    var segments = prediction();
    if (segments.isEmpty()) return;
    var source = m.renderBuffers().bufferSource();
    var out = source.getBuffer(RenderType.lines());
    var pose = e.getPoseStack();
    pose.pushPose();
    pose.translate(-camera.x, -camera.y, -camera.z);
    for (var s : segments) {
      int color = s.bounced ? 0xcc70e6dc : 0xbbf2bd72;
      line(pose, out, s.from, s.to, color);
    }
    for (int i = 1; i < segments.size(); i++)
      if (segments.get(i).bounced && !segments.get(i - 1).bounced) {
        Vec3 p = segments.get(i - 1).to;
        line(pose, out, p.add(-.12, 0, 0), p.add(.12, 0, 0), 0xfff2bd72);
        line(pose, out, p.add(0, -.12, 0), p.add(0, .12, 0), 0xfff2bd72);
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
          () -> {
            CyberwareNetwork.trajectory = dev.cybernetics2077.client.GuidedBulletClient::receive;
            CyberwareNetwork.flight = dev.cybernetics2077.client.SmoothBulletClient::receive;
            CyberwareNetwork.rebound = dev.cybernetics2077.client.ReboundTrailClient::receive;
          });
    }
  }

  private HandsClient() {}
}
