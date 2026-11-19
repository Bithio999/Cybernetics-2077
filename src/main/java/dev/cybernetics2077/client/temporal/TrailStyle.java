package dev.cybernetics2077.client.temporal;

/** Time-based spectrum and opacity envelopes are independent, so the tail keeps its colors. */
public final class TrailStyle {
  public static final double WORLD_LIFETIME = 42, WORLD_INTERVAL = 3.5;
  public static final int WORLD_HISTORY = 12, WORLD_DRAW_LIMIT = 48;
  public static final double HAND_LIFETIME = 12, HAND_INTERVAL = 3;
  public static final int HAND_HISTORY = 4;
  private static final int[] SPECTRUM = {
    0xff9c3b, 0xffd447, 0xd9ff50, 0x79ff60, 0x32f69e, 0x25e6cf, 0x32ccff, 0x578aff, 0x8868ff,
    0xc65bff, 0xff65cb
  };

  private static double ease(double low, double high, double value) {
    double t = Math.clamp((value - low) / (high - low), 0, 1);
    return t * t * (3 - 2 * t);
  }

  public static int color(double age, boolean hands) {
    double life = hands ? HAND_LIFETIME : WORLD_LIFETIME;
    double progress = Math.clamp((age - (hands ? 1.5 : 3)) / (hands ? 7.5 : 28), 0, 1);
    double stop = progress * (SPECTRUM.length - 1);
    int index = Math.min(SPECTRUM.length - 2, (int) stop);
    double blend = ease(0, 1, stop - index);
    int a = SPECTRUM[index], b = SPECTRUM[index + 1], rgb = 0;
    for (int shift = 0; shift <= 16; shift += 8)
      rgb |=
          (int) Math.round(((a >> shift) & 255) * (1 - blend) + ((b >> shift) & 255) * blend)
              << shift;
    double opacity =
        ease(hands ? 1 : 1.5, hands ? 2.5 : 4, age) * (1 - ease(hands ? 8 : 36, life, age));
    return ((int) Math.round((hands ? 58 : 140) * opacity) << 24) | rgb;
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
