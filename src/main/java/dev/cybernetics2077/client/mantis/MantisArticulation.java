package dev.cybernetics2077.client.mantis;

import org.joml.Matrix4f;

/**
 * Two real hinges: support goes back/up, cutting assembly counter-rotates to point past the hand.
 */
public final class MantisArticulation {
  public static final float SUPPORT_LENGTH = .18f;

  public static Matrix4f mount(float open) {
    return new Matrix4f().translate(0, .455f, -.065f - .085f * open);
  }

  public static Matrix4f support(float hinge) {
    return new Matrix4f().rotateX((float) Math.toRadians(-180 + 35 * hinge));
  }

  public static Matrix4f blade(float hinge, float extension) {
    float support = -180 + 35 * hinge, absolute = -8 * extension;
    return new Matrix4f()
        .translate(0, SUPPORT_LENGTH, 0)
        .rotateX((float) Math.toRadians(absolute - support));
  }

  public static Matrix4f tip(float extension) {
    return new Matrix4f()
        .translate(0, .012f + .19f * MantisMotion.smooth(Math.clamp(extension / .7f, 0, 1)), 0);
  }

  public static Matrix4f edge(float extension) {
    return new Matrix4f()
        .translate(
            0, .006f + .19f * MantisMotion.smooth(Math.clamp((extension - .3f) / .7f, 0, 1)), 0);
  }

  private MantisArticulation() {}
}
