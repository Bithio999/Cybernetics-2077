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
