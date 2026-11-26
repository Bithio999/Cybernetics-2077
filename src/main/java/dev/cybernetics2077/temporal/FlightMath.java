package dev.cybernetics2077.temporal;

/** Fractional powers of TaCZ's native move-then-drag/gravity step. Units stay in native ticks. */
public final class FlightMath {
  public record Vector(double x, double y, double z) {}

  public static double drag(double q, double time) {
    return Math.pow(q, time);
  }

  public static double sum(double q, double time) {
    double d = 1 - q;
    if (d < 1e-4)
      return time + time * (1 - time) * d / 2 + time * (1 - time) * (2 - time) * d * d / 6;
    return -Math.expm1(Math.log(q) * time) / d;
  }

  public static Vector displacement(
      double x, double y, double z, double q, double gravity, double time) {
    if (time == 1) return new Vector(x, y, z);
    double d = 1 - q, a = sum(q, time);
    double b =
        d < 1e-4 ? time * (1 - time) / 2 + time * (1 - time) * (2 - time) * d / 6 : (a - time) / d;
    return new Vector(x * a, y * a + gravity * b, z * a);
  }

  public static boolean mechanism(String name) {
    var n = name.toLowerCase(java.util.Locale.ROOT);
    if (n.contains("reload")
        || n.startsWith("static_")
        || n.startsWith("inspect")
        || n.startsWith("switch_")
        || n.startsWith("draw")
        || n.startsWith("put_")) return false;
    return n.contains("shoot")
        || n.contains("fire")
        || n.contains("bolt")
        || n.contains("pump")
        || n.contains("cycle")
        || n.contains("cock");
  }

  private FlightMath() {}
}
