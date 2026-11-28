package dev.cybernetics2077.temporal;

import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.compat.TacZCompat;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Kinetic rounds tick every world tick; native collision callbacks still own every hit. */
public final class BulletTime {
  private static final class Clock {
    double age;

    Clock(double age) {
      this.age = age;
    }
  }

  private static final class Frame {
    final Entity entity;
    final double dt;
    final Vec3 start;
    Vec3 movement, hit;
    double q = 1;
    boolean bounce;

    Frame(Entity e, double dt) {
      entity = e;
      this.dt = dt;
      start = e.position();
    }
  }

  private static final ThreadLocal<Map<Entity, Clock>> CLOCKS =
      ThreadLocal.withInitial(WeakHashMap::new);
  private static final ThreadLocal<Frame> FRAME = new ThreadLocal<>();

  public static boolean eligible(Entity e) {
    return e instanceof BulletFlightAccess a
        && TacZCompat.kinetic(e)
        && Float.isFinite(a.cybernetics$friction())
        && a.cybernetics$friction() >= 0
        && a.cybernetics$friction() < 1
        && Float.isFinite(a.cybernetics$gravity());
  }

  public static double age(Entity e) {
    var c = CLOCKS.get().get(e);
    return c == null ? e.tickCount : c.age;
  }

  public static void tick(Entity e, Runnable nativeTick) {
    if (!eligible(e)) {
      nativeTick.run();
      return;
    }
    double dt = Math.clamp(Sandevistan.pace(e), TemporalRules.MIN_PACE, 1);
    var clock = CLOCKS.get().get(e);
    if (clock == null && dt >= 1) {
      nativeTick.run();
      return;
    }
    if (clock == null) {
      clock = new Clock(Math.max(0, e.tickCount - 1));
      CLOCKS.get().put(e, clock);
    }
    clock.age += dt;
    var previous = FRAME.get();
    var frame = new Frame(e, dt);
    FRAME.set(frame);
    try {
      nativeTick.run();
    } finally {
      if (e.level() instanceof ServerLevel level) {
        Vec3 end = e.isRemoved() && frame.hit != null ? frame.hit : e.position(),
            v = e.getDeltaMovement();
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            e,
            new CyberwareNetwork.Flight(
                e.getId(),
                e.getUUID(),
                level.getGameTime(),
                frame.start.x,
                frame.start.y,
                frame.start.z,
                end.x,
                end.y,
                end.z,
                v.x,
                v.y,
                v.z,
                (float) dt,
                (float) clock.age,
                e.isRemoved(),
                frame.bounce));
      }
      if (e.isRemoved()) CLOCKS.get().remove(e);
      if (previous == null) FRAME.remove();
      else FRAME.set(previous);
    }
  }

  private static Frame frame(Entity e) {
    var f = FRAME.get();
    return f != null && f.entity == e ? f : null;
  }

  public static Vec3 motion(Entity e, Vec3 velocity, float friction, float gravity) {
    var f = frame(e);
    if (f == null) return velocity;
    float drag = e.isInWater() ? .4f : friction;
    double g = e.isInWater() ? gravity * .6f : gravity;
    f.q = 1f - drag;
    var d = FlightMath.displacement(velocity.x, velocity.y, velocity.z, f.q, g, f.dt);
    return f.movement = new Vec3(d.x(), d.y(), d.z());
  }

  public static double drag(Entity e, double nativeFactor) {
    var f = frame(e);
    return f == null ? nativeFactor : FlightMath.drag(nativeFactor, f.dt);
  }

  public static double gravity(Entity e, double nativeGravity) {
    var f = frame(e);
    return f == null ? nativeGravity : nativeGravity * FlightMath.sum(f.q, f.dt);
  }

  public static int nativeAge(Entity e, int ordinary) {
    return frame(e) == null ? ordinary : (int) Math.floor(age(e) + 1e-8);
  }

  public static Vec3 end(Entity e) {
    var f = frame(e);
    return e.position().add(f == null || f.movement == null ? e.getDeltaMovement() : f.movement);
  }

  public static void hit(Entity e, Vec3 p) {
    var f = frame(e);
    if (f != null) f.hit = p;
  }

  public static void bounce(Entity e) {
    var f = frame(e);
    if (f != null) f.bounce = true;
  }

  private BulletTime() {}
}
