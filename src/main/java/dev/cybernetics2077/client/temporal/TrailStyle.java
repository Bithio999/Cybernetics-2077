package dev.cybernetics2077.client.temporal;

/** Capture hue once; only opacity changes while the saved pose ages. */
public final class TrailStyle {
  public static final double WORLD_LIFETIME = 42, WORLD_INTERVAL = 3.5;
  public static final int WORLD_HISTORY = 12, WORLD_DRAW_LIMIT = 48;
  public static final double HAND_LIFETIME = 12, HAND_INTERVAL = 3;
  public static final int HAND_HISTORY = 4;
  public static final double COLOR_INTERVAL = 6;
  private static final int[] SPECTRUM = {
    0x31e7ae, 0x2edbd4, 0x41c7e5, 0x459fdf, 0x546dd0, 0x856cc8, 0xbb78a9, 0xd88a85, 0xe2ad70,
    0xe0d17b, 0x79dc93
  };

  private static double ease(double low, double high, double value) {
    double t = Math.clamp((value - low) / (high - low), 0, 1);
    return t * t * (3 - 2 * t);
  }

  public static final double COLOR_CYCLE = SPECTRUM.length * COLOR_INTERVAL;

  /** Birth time and RGB belong to the captured pose, not the frame drawing it. */
  public record PoseTint(double bornAt, int rgb) {
    public double age(double now) {
      return Math.max(0, now - bornAt);
    }

    public int color(double now, boolean hands) {
      double age = age(now), life = hands ? HAND_LIFETIME : WORLD_LIFETIME;
      double opacity =
          ease(hands ? 1 : 1.5, hands ? 2.5 : 4, age) * (1 - ease(hands ? 8 : 36, life, age));
      return ((int) Math.round((hands ? 58 : 140) * opacity) << 24) | rgb;
    }
  }

  /** Each activation starts mint/cyan; later captures advance around the palette. */
  public static PoseTint capture(double now, double activationAge) {
    double phase = Math.max(0, activationAge) % COLOR_CYCLE / COLOR_INTERVAL;
    int index = (int) phase;
    double blend = ease(0, 1, phase - index);
    int a = SPECTRUM[index], b = SPECTRUM[(index + 1) % SPECTRUM.length], rgb = 0;
    for (int shift = 0; shift <= 16; shift += 8)
      rgb |=
          (int) Math.round(((a >> shift) & 255) * (1 - blend) + ((b >> shift) & 255) * blend)
              << shift;
    return new PoseTint(now, rgb);
  }

  public static boolean bodyVisible(boolean own, boolean firstPerson) {
    return !own || !firstPerson;
  }

  /** Draw samples across the complete history, retaining both head and tail at lower detail. */
  public static boolean selected(int index, int count, int budget) {
    if (budget <= 0) return false;
    if (count <= budget) return true;
    if (budget == 1) return index == count - 1;
    for (int i = 0; i < budget; i++)
      if ((int) Math.round(i * (count - 1.) / (budget - 1)) == index) return true;
    return false;
  }

  private TrailStyle() {}
}
