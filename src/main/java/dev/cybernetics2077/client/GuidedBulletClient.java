package dev.cybernetics2077.client;

import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.temporal.Sandevistan;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** A single server-controlled projectile pose; never creates a second projectile or damage hit. */
@EventBusSubscriber(modid = "cybernetics2077", value = Dist.CLIENT)
public final class GuidedBulletClient {
  public static final String MANAGED = "cybernetics2077:guided_visual";

  private record Sample(long tick, Vec3 position, Vec3 velocity) {}

  private static final class Track {
    Sample older, latest;
  }

  private static final Map<Entity, Track> TRACKS = new WeakHashMap<>();
  private static Object level;

  public static boolean managed(Entity e) {
    return e.level().isClientSide && e.getPersistentData().getBoolean(MANAGED);
  }

  public static void receive(CyberwareNetwork.Ballistics p) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (level != mc.level) {
      TRACKS.clear();
      level = mc.level;
    }
    var e = mc.level.getEntity(p.entity());
    if (e == null || !e.getUUID().equals(p.uuid()) || !TacZCompat.bullet(e)) return;
    var track = TRACKS.computeIfAbsent(e, k -> new Track());
    long tick = p.tick() < 0 ? mc.level.getGameTime() : p.tick();
    if (track.latest != null && tick < track.latest.tick) return;
    track.older = track.latest;
    track.latest =
        new Sample(tick, new Vec3(p.x(), p.y(), p.z()), new Vec3(p.vx(), p.vy(), p.vz()));
    e.getPersistentData().putBoolean(MANAGED, true);
    if (track.older == null) {
      e.setPos(track.latest.position);
      e.setOldPosAndRot();
    }
    e.setDeltaMovement(track.latest.velocity);
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (mc.level != level) {
      TRACKS.clear();
      level = mc.level;
    }
    if (mc.level == null || mc.isPaused()) return;
    double now = mc.level.getGameTime() - 2;
    var it = TRACKS.entrySet().iterator();
    while (it.hasNext()) {
      var entry = it.next();
      var e = entry.getKey();
      var t = entry.getValue();
      if (e.isRemoved() || e.level() != mc.level) {
        it.remove();
        continue;
      }
      Vec3 point;
      if (t.older != null && t.latest.tick > t.older.tick && now <= t.latest.tick)
        point =
            t.older.position.lerp(
                t.latest.position,
                Math.clamp((now - t.older.tick) / (t.latest.tick - t.older.tick), 0, 1));
      else
        point =
            t.latest.position.add(
                t.latest.velocity.scale(
                    Math.clamp((now - t.latest.tick) * Sandevistan.pace(e), 0, 2)));
      var direction = t.latest.velocity;
      e.setOldPosAndRot();
      e.setPos(point);
      e.setDeltaMovement(t.latest.velocity);
      if (direction.lengthSqr() > 1e-8) {
        e.setYRot((float) Math.toDegrees(Math.atan2(direction.x, direction.z)));
        e.setXRot((float) Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
      }
    }
  }

  private GuidedBulletClient() {}
}
