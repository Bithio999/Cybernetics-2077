package dev.cybernetics2077.combat;

/** Pure, bounded projectile math shared by server guidance and the trajectory preview. */
public final class HandsRules {
  public record Vector(double x, double y, double z) {}

  public static Vector reflect(double x, double y, double z, double nx, double ny, double nz) {
    double dot = x * nx + y * ny + z * nz;
    return new Vector(x - 2 * dot * nx, y - 2 * dot * ny, z - 2 * dot * nz);
  }

  public static Vector turn(
      double x, double y, double z, double dx, double dy, double dz, double radians) {
    double speed = Math.sqrt(x * x + y * y + z * z),
        length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (speed < 1e-8 || length < 1e-8) return new Vector(x, y, z);
    double ax = x / speed,
        ay = y / speed,
        az = z / speed,
        bx = dx / length,
        by = dy / length,
        bz = dz / length;
    double angle = Math.acos(Math.clamp(ax * bx + ay * by + az * bz, -1, 1));
    if (angle < 1e-6) return new Vector(x, y, z);
    // Never flip a projectile backward; a lost/behind target breaks guidance instead.
    if (angle > Math.PI * .49) return new Vector(x, y, z);
    double t = Math.min(1, Math.max(0, radians) / angle), sin = Math.sin(angle);
    double a = Math.sin((1 - t) * angle) / sin, b = Math.sin(t * angle) / sin;
    return new Vector(
        (ax * a + bx * b) * speed, (ay * a + by * b) * speed, (az * a + bz * b) * speed);
  }

  /** Constant-velocity intercept with a short lead horizon; preserves projectile speed. */
  public static Vector guide(
      double vx,
      double vy,
      double vz,
      double dx,
      double dy,
      double dz,
      double tx,
      double ty,
      double tz,
      int tier,
      double step) {
    double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
    double a = tx * tx + ty * ty + tz * tz - speed * speed;
    double b = 2 * (dx * tx + dy * ty + dz * tz), c = dx * dx + dy * dy + dz * dz;
    double time = speed > 1e-8 ? Math.sqrt(c) / speed : 0;
    double disc = b * b - 4 * a * c;
    if (Math.abs(a) > 1e-8 && disc >= 0) {
      double one = (-b - Math.sqrt(disc)) / (2 * a), two = (-b + Math.sqrt(disc)) / (2 * a);
      if (one > 0 && two > 0) time = Math.min(one, two);
      else if (one > 0 || two > 0) time = Math.max(one, two);
    }
    time = Math.clamp(time, 0, 8);
    double movement = Math.sqrt(tx * tx + ty * ty + tz * tz);
    if (movement * time > 4) time = 4 / movement;
    return turn(
        vx,
        vy,
        vz,
        dx + tx * time,
        dy + ty * time,
        dz + tz * time,
        (.14 + .035 * Math.clamp(tier, 1, 5)) * Math.clamp(step, 0, 1));
  }

  public static double bounceSpeed(int tier) {
    return .60 + .05 * Math.clamp(tier, 1, 5);
  }

  public static int chargePeriod(int tier) {
    return Math.max(2, 6 - Math.clamp(tier, 2, 5));
  }

  public static double gravityReduction(int tier) {
    return tier <= 0 ? 0 : .2 + .08 * Math.clamp(tier, 2, 5);
  }

  public static double spreadMultiplier(int tier) {
    return 1 - .08 * Math.clamp(tier, 0, 5);
  }

  private HandsRules() {}
}
