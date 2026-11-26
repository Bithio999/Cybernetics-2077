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

  private record Pending(CyberwareNetwork.Ballistics packet, long born) {}

  private static final Map<java.util.UUID, Pending> PENDING = new LinkedHashMap<>();
  private static Object level;

  public static boolean managed(Entity e) {
    return e.level().isClientSide && e.getPersistentData().getBoolean(MANAGED);
  }

  public static void receive(CyberwareNetwork.Ballistics p) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (level != mc.level) {
      TRACKS.clear();
      PENDING.clear();
      level = mc.level;
    }
    var e = mc.level.getEntity(p.entity());
    if (e == null) {
      PENDING.put(p.uuid(), new Pending(p, mc.level.getGameTime()));
      while (PENDING.size() > 128) PENDING.remove(PENDING.keySet().iterator().next());
      return;
    }
    if (!e.getUUID().equals(p.uuid()) || !TacZCompat.bullet(e)) return;
    apply(e, p);
  }

  private static void apply(Entity e, CyberwareNetwork.Ballistics p) {
    var mc = Minecraft.getInstance();
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

  /** Stop local straight-flight prediction at the actual wall while awaiting the server bounce. */
  public static boolean guard(Entity e) {
    if (managed(e)) return true;
    var mc = Minecraft.getInstance();
    if (mc.level == null
        || e.level() != mc.level
        || !(e instanceof net.minecraft.world.entity.projectile.Projectile projectile)
        || !(projectile.getOwner() instanceof net.minecraft.world.entity.player.Player owner)
        || dev.cybernetics2077.client.mantis.CyberwareClient.state(owner.getId())
                .getInt("ballisticTier")
            == 0
        || !TacZCompat.kinetic(e)
        || e.getDeltaMovement().lengthSqr() < .04) return false;
    var hit = TacZCompat.clip(mc.level, e.position(), e.position().add(e.getDeltaMovement()), e);
    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return false;
    e.setPos(
        hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(.08)));
    return true;
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    var mc = Minecraft.getInstance();
    if (mc.level != level) {
      TRACKS.clear();
      PENDING.clear();
      level = mc.level;
    }
    if (mc.level == null || mc.isPaused()) return;
    var waiting = PENDING.entrySet().iterator();
    while (waiting.hasNext()) {
      var entry = waiting.next();
      var p = entry.getValue().packet;
      var entity = mc.level.getEntity(p.entity());
      if (entity != null) {
        if (entity.getUUID().equals(p.uuid()) && TacZCompat.bullet(entity)) apply(entity, p);
        waiting.remove();
      } else if (mc.level.getGameTime() - entry.getValue().born >= 40) waiting.remove();
    }
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
