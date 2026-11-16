package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.cyberware.CyberwareCategory;

/** Camera and viewport move together; category changes never snap the model. */
public record BodyView(
    float x, float y, float w, float h, float centerX, float centerY, float span, float yaw) {
  public static final BodyView OVERVIEW = new BodyView(736, 100, 448, 830, 0, .53f, 2.28f, -12);

  public static BodyView category(CyberwareCategory category) {
    float cx = 0, cy = .32f, span = 1.27f, yaw = -10;
    switch (category) {
      case FACE, FRONTAL_CORTEX -> {
        cy = -.235f;
        span = .80f;
        yaw = -6;
      }
      case OPERATING_SYSTEM -> {
        cy = .18f;
        span = 1.07f;
      }
      case SKELETON, CIRCULATORY_SYSTEM -> {
        cy = .39f;
        span = 1.18f;
      }
      case NERVOUS_SYSTEM -> {
        cy = .34f;
        span = 1.20f;
        yaw = 170;
      }
      case ARMS -> {
        cx = -.34f;
        cy = .32f;
        span = 1.18f;
      }
      case HANDS -> {
        cx = -.35f;
        cy = .69f;
        span = .64f;
        yaw = -15;
      }
      case INTEGUMENTARY_SYSTEM -> {
        cx = -.21f;
        cy = .18f;
        span = .78f;
        yaw = -28;
      }
      case LEGS -> {
        cy = 1.10f;
        span = 1.04f;
      }
    }
    return new BodyView(1000, 146, 630, 740, cx, cy, span, yaw);
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
