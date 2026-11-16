package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.cyberware.CyberwareCategory;
import org.joml.Matrix4f;

/** Uniform world scale throughout overview/category transitions, with model-space focus points. */
public record BodyView(
    float x, float y, float w, float h, float centerX, float centerY, float span, float yaw) {
  public static final BodyView OVERVIEW = new BodyView(736, 100, 448, 830, 0, .5f, 2.28f, 0);

  public static BodyView category(CyberwareCategory category) {
    return category(category, false);
  }

  public static BodyView category(CyberwareCategory category, boolean slim) {
    float cx = 0, cy = .375f, span = 1.10f, yaw = -10;
    float arm = slim ? -.34375f : -.375f;
    switch (category) {
      case FACE, FRONTAL_CORTEX -> {
        cy = -.25f;
        span = .80f;
        yaw = 0;
      }
      case OPERATING_SYSTEM -> {
        cy = .1875f;
        span = 1.07f;
      }
      case SKELETON, CIRCULATORY_SYSTEM -> {
        cy = .375f;
        span = 1.10f;
      }
      case NERVOUS_SYSTEM -> {
        cy = .375f;
        span = 1.20f;
        yaw = 170;
      }
      case ARMS -> {
        cx = arm;
        cy = slim ? .40625f : .375f;
        span = 1.05f;
      }
      case HANDS -> {
        cx = arm;
        cy = slim ? .71875f : .6875f;
        span = .64f;
        yaw = -15;
      }
      case INTEGUMENTARY_SYSTEM -> {
        cx = arm;
        cy = slim ? .1875f : .15625f;
        span = .78f;
        yaw = -28;
      }
      case LEGS -> {
        cy = 1.125f;
        span = 1.04f;
      }
    }
    return new BodyView(1000, 146, 630, 740, cx, cy, span, yaw);
  }

  public Matrix4f projection(float pointerYaw) {
    float horizontal = span * w / h;
    float rotatedCenterX = centerX * (float) Math.cos(Math.toRadians(yaw + pointerYaw));
    return new Matrix4f()
        .setOrtho(
            rotatedCenterX - horizontal / 2,
            rotatedCenterX + horizontal / 2,
            centerY + span / 2,
            centerY - span / 2,
            10,
            -10);
  }

  public float canvasY(float modelY) {
    return y + h * (.5f + (modelY - centerY) / span);
  }

  public BodyView towards(BodyView b, float t) {
    return new BodyView(
        mix(x, b.x, t),
        mix(y, b.y, t),
        mix(w, b.w, t),
        mix(h, b.h, t),
        mix(centerX, b.centerX, t),
        mix(centerY, b.centerY, t),
        mix(span, b.span, t),
        mix(yaw, b.yaw, t));
  }

  private static float mix(float a, float b, float t) {
    return a + (b - a) * t;
  }
}
