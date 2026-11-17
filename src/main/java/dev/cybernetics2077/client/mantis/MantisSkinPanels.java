package dev.cybernetics2077.client.mantis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joml.Matrix4f;

/** Separate dermal plates and side leaves, rather than a pair of long hatch doors. */
public final class MantisSkinPanels {
  public static final float START = -.055f, END = .53f, SIDE_END = .50f, WIDTH = .49f;

  public enum Panel {
    PROXIMAL,
    MID_LEFT,
    MID_RIGHT,
    CARRIER_LEFT,
    CARRIER_RIGHT,
    WRIST,
    SIDE_LEFT,
    SIDE_RIGHT
  }

  public static final Panel[] ALL = Panel.values();

  public record Region(
      float x,
      float X,
      float y,
      float Y,
      float z,
      float Z,
      float slope,
      float intercept,
      int split) {
    public boolean contains(float a, float b, float c) {
      return a >= x
          && a <= X
          && b >= y
          && b <= Y
          && c >= z
          && c <= Z
          && (split == 0 || (a - slope * b - intercept) * split >= 0);
    }
  }

  private static final Map<Float, Region[]> REGIONS = new ConcurrentHashMap<>();

  public static Region region(Panel panel, float width) {
    return REGIONS
        .computeIfAbsent(
            width,
            w -> {
              float b = w * WIDTH;
              return new Region[] {
                new Region(-b, b, START, MantisRig.ELBOW_Y, -1, 1, 0, 0, 0),
                new Region(-b, b, MantisRig.ELBOW_Y, .31f, -1, 1, .12f, -.024f, -1),
                new Region(-b, b, MantisRig.ELBOW_Y, .31f, -1, 1, .12f, -.024f, 1),
                new Region(-b, b, .31f, .44f, -1, 1, -.10f, .0375f, -1),
                new Region(-b, b, .31f, .44f, -1, 1, -.10f, .0375f, 1),
                new Region(-b, b, .44f, END, -1, 1, 0, 0, 0),
                new Region(-1, 1, MantisRig.ELBOW_Y, SIDE_END, -1, -.01f, 0, 0, 0),
                new Region(-1, 1, MantisRig.ELBOW_Y, SIDE_END, -1, -.01f, 0, 0, 0)
              };
            })[panel.ordinal()];
  }

  private static float phase(float p, float a, float b) {
    return MantisMotion.smooth(Math.clamp((p - a) / (b - a), 0, 1));
  }

  public static Matrix4f transform(Panel panel, float width, float open, boolean sleeve) {
    float z = -.125f - (sleeve ? .015625f : 0), b = width * WIDTH;
    var m = new Matrix4f();
    switch (panel) {
      case PROXIMAL -> {
        float t = phase(open, 0, .50f);
        m.translate(0, -.15f * t, -.02f * t)
            .translate(0, START, z)
            .rotateX((float) Math.toRadians(-25 * t))
            .translate(0, -START, -z);
      }
      case MID_LEFT, MID_RIGHT -> {
        float side = panel == Panel.MID_LEFT ? -1 : 1, t = phase(open, .08f, .80f);
        m.translate(side * .05f * t, -.014f * t, -.026f * t)
            .translate(side * b, .20f, z)
            .rotateY((float) Math.toRadians(-side * 62 * t))
            .rotateX((float) Math.toRadians(-12 * t))
            .translate(-side * b, -.20f, -z);
      }
      case CARRIER_LEFT, CARRIER_RIGHT -> {
        float side = panel == Panel.CARRIER_LEFT ? -1 : 1, t = phase(open, .24f, .96f);
        m.translate(side * .05f * t, .022f * t, -.015f * t)
            .translate(side * b, .44f, z)
            .rotateY((float) Math.toRadians(-side * 78 * t))
            .rotateX((float) Math.toRadians(18 * t))
            .translate(-side * b, -.44f, -z);
      }
      case WRIST -> {
        float t = phase(open, .04f, .68f);
        m.translate(0, .08f * t, -.015f * t)
            .translate(0, END, z)
            .rotateX((float) Math.toRadians(35 * t))
            .translate(0, -END, -z);
      }
      case SIDE_LEFT, SIDE_RIGHT -> {
        float side = panel == Panel.SIDE_LEFT ? -1 : 1, t = phase(open, .12f, .86f);
        float edge = side * (width / 2 + (sleeve ? .015625f : 0));
        m.translate(side * .035f * t, 0, .006f * t)
            .translate(edge, 0, .02f)
            .rotateY((float) Math.toRadians(-side * 32 * t))
            .translate(-edge, 0, -.02f);
      }
    }
    return m;
  }

  private MantisSkinPanels() {}
}
