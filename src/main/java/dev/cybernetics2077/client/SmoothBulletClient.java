package dev.cybernetics2077.client;

import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.compat.TacZCompat;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * One current world-tick sweep, rendered with vanilla frame interpolation and no sparse-tick clock.
 */
@EventBusSubscriber(modid = "cybernetics2077", value = Dist.CLIENT)
public final class SmoothBulletClient {
  private static final class Track {
    CyberwareNetwork.Flight packet;
    long applied = Long.MIN_VALUE;
  }

  private record Pending(CyberwareNetwork.Flight packet, long born) {}

  private static final Map<Entity, Track> TRACKS = new WeakHashMap<>();
  private static final Map<UUID, Pending> PENDING = new LinkedHashMap<>();
  private static Object level;

  private static void world() {
    var current = Minecraft.getInstance().level;
    if (current != level) {
      TRACKS.clear();
      PENDING.clear();
      level = current;
    }
  }

  public static double visualPace(Entity e) {
    var track = TRACKS.get(e);
    return track == null ? 1 : track.packet.pace();
  }

  public static boolean owns(UUID uuid) {
    world();
    return PENDING.containsKey(uuid)
        || TRACKS.keySet().stream().anyMatch(e -> e.getUUID().equals(uuid));
  }

  public static void receive(CyberwareNetwork.Flight p) {
    world();
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    var e = mc.level.getEntity(p.entity());
    if (e == null) {
      PENDING.put(p.uuid(), new Pending(p, mc.level.getGameTime()));
      while (PENDING.size() > 128) PENDING.remove(PENDING.keySet().iterator().next());
      return;
    }
    if (!e.getUUID().equals(p.uuid()) || !TacZCompat.bullet(e)) return;
    apply(e, p);
  }

  private static void apply(Entity e, CyberwareNetwork.Flight p) {
    var track = TRACKS.computeIfAbsent(e, k -> new Track());
    if (track.packet != null && p.tick() < track.packet.tick()) return;
    GuidedBulletClient.release(e);
    e.getPersistentData().putBoolean(GuidedBulletClient.MANAGED, true);
    track.packet = p;
    if (p.ended()) {
      e.setPos(p.x(), p.y(), p.z());
      e.setOldPosAndRot();
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post event) {
    world();
    var mc = Minecraft.getInstance();
    if (mc.level == null || mc.isPaused()) return;
    var pending = PENDING.entrySet().iterator();
    while (pending.hasNext()) {
      var p = pending.next().getValue();
      var e = mc.level.getEntity(p.packet.entity());
      if (e != null) {
        if (e.getUUID().equals(p.packet.uuid()) && TacZCompat.bullet(e)) apply(e, p.packet);
        pending.remove();
      } else if (mc.level.getGameTime() - p.born > 40) pending.remove();
    }
    var it = TRACKS.entrySet().iterator();
    while (it.hasNext()) {
      var entry = it.next();
      var e = entry.getKey();
      var track = entry.getValue();
      if (e.isRemoved() || e.level() != mc.level) {
        it.remove();
        continue;
      }
      var p = track.packet;
      if (track.applied == p.tick()) {
        e.setOldPosAndRot();
        continue;
      }
      boolean first = track.applied == Long.MIN_VALUE;
      Vec3 from = first ? new Vec3(p.px(), p.py(), p.pz()) : e.position(),
          to = new Vec3(p.x(), p.y(), p.z());
      // A collision endpoint is a waypoint; do not smooth a correction across its surface.
      if (p.bounced() || p.ended()) from = new Vec3(p.px(), p.py(), p.pz());
      e.setPos(from);
      e.setOldPosAndRot();
      e.setPos(to);
      e.setDeltaMovement(p.vx(), p.vy(), p.vz());
      Vec3 d = to.subtract(from);
      if (d.lengthSqr() > 1e-10) {
        float yaw = (float) Math.toDegrees(Math.atan2(d.x, d.z));
        e.setYRot(e.yRotO + net.minecraft.util.Mth.wrapDegrees(yaw - e.yRotO));
        e.setXRot((float) Math.toDegrees(Math.atan2(d.y, d.horizontalDistance())));
      }
      track.applied = p.tick();
    }
  }

  private SmoothBulletClient() {}
}
