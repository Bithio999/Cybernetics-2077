package dev.cybernetics2077.client;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.combat.CyberwareNetwork;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Short, depth-tested traces of completed server flight; independent of projectile removal. */
@EventBusSubscriber(modid = "cybernetics2077", value = Dist.CLIENT)
public final class ReboundTrailClient {
  private record Trail(Vec3 from, Vec3 to, long born) {}

  private static final ArrayDeque<Trail> TRAILS = new ArrayDeque<>();
  private static final Map<UUID, Long> COVERED = new LinkedHashMap<>();
  private static Object level;

  private static void world() {
    var current = Minecraft.getInstance().level;
    if (current != level) {
      TRAILS.clear();
      COVERED.clear();
      level = current;
    }
  }

  public static void receive(CyberwareNetwork.Rebound p) {
    world();
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    long now = mc.level.getGameTime();
    var from = new Vec3(p.x(), p.y(), p.z());
    var to = new Vec3(p.tx(), p.ty(), p.tz());
    if (!Double.isFinite(from.lengthSqr()) || !Double.isFinite(to.lengthSqr())) return;
    COVERED.put(p.uuid(), now + 4);
    while (COVERED.size() > 128) COVERED.remove(COVERED.keySet().iterator().next());
    if (from.distanceToSqr(to) < .0001) return;
    // Limit the visible length of unusually large native steps.
    Vec3 delta = to.subtract(from);
    double length = delta.length();
    to = from.add(delta.scale(Math.min(1, 160 / length)));
    TRAILS.addLast(new Trail(from, to, now));
    while (TRAILS.size() > 128) TRAILS.removeFirst();
  }

  public static boolean covers(Entity entity) {
    world();
    var mc = Minecraft.getInstance();
    return mc.level != null
        && COVERED.getOrDefault(entity.getUUID(), Long.MIN_VALUE) > mc.level.getGameTime();
  }

  public static boolean covers(UUID uuid) {
    world();
    var mc = Minecraft.getInstance();
    return mc.level != null && COVERED.getOrDefault(uuid, Long.MIN_VALUE) > mc.level.getGameTime();
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    world();
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    long now = mc.level.getGameTime();
    while (!TRAILS.isEmpty() && now - TRAILS.peekFirst().born >= 4) TRAILS.removeFirst();
    COVERED.values().removeIf(until -> until <= now);
  }

  @SubscribeEvent
  public static void render(RenderLevelStageEvent event) {
    if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
    world();
    var mc = Minecraft.getInstance();
    if (mc.level == null || TRAILS.isEmpty()) return;
    double now = mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
    var camera = event.getCamera().getPosition();
    var pose = event.getPoseStack();
    pose.pushPose();
    var source = mc.renderBuffers().bufferSource();
    var out = source.getBuffer(RenderType.debugQuads());
    for (var trail : TRAILS) {
      float fade = (float) Math.clamp(1 - (now - trail.born) / 4, 0, 1);
      if (fade <= 0) continue;
      Vec3 axis = trail.to.subtract(trail.from).normalize();
      Vec3 side = axis.cross(camera.subtract(trail.from.lerp(trail.to, .5))).normalize();
      if (side.lengthSqr() < .0001) continue;
      ribbon(pose, out, trail.from, trail.to, side.scale(.026), camera, .2f * fade, 0x65dbe3);
      ribbon(pose, out, trail.from, trail.to, side.scale(.008), camera, .85f * fade, 0xe9fff7);
    }
    pose.popPose();
    source.endBatch(RenderType.debugQuads());
  }

  private static void ribbon(
      PoseStack pose,
      VertexConsumer out,
      Vec3 a,
      Vec3 b,
      Vec3 side,
      Vec3 camera,
      float alpha,
      int rgb) {
    var matrix = pose.last().pose();
    int color = (Math.round(alpha * 255) << 24) | rgb;
    // Subtract in double precision so thin traces remain stable far from world origin.
    for (var p : new Vec3[] {a.subtract(side), a.add(side), b.add(side), b.subtract(side)}) {
      var relative = p.subtract(camera);
      out.addVertex(matrix, (float) relative.x, (float) relative.y, (float) relative.z)
          .setColor(color);
    }
  }

  private ReboundTrailClient() {}
}
