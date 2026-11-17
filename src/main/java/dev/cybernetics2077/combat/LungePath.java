package dev.cybernetics2077.combat;

/** A short leap, sampled as ordinary collision-tested movement on the owning client. */
public record LungePath(
    double sx, double sy, double sz, double ex, double ey, double ez, double arc, int ticks) {
  public record Point(double x, double y, double z) {}

  public Point sample(double age) {
    double t = Math.clamp(age / ticks, 0, 1);
    return new Point(
        sx + (ex - sx) * t, sy + (ey - sy) * t + Math.sin(Math.PI * t) * arc, sz + (ez - sz) * t);
  }

  public Point step(double x, double y, double z, double age) {
    var next = sample(age + 1);
    double dx = next.x - x,
        dy = next.y - y,
        dz = next.z - z,
        len = Math.sqrt(dx * dx + dy * dy + dz * dz);
    double factor = len > 1.35 ? 1.35 / len : 1;
    return new Point(dx * factor, dy * factor, dz * factor);
  }

  public static int duration(double distance) {
    return (int) Math.clamp(Math.ceil(distance / .9) + 1, 5, 12);
  }
}
