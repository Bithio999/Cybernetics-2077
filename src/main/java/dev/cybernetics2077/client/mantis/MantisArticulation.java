package dev.cybernetics2077.client.mantis;

import org.joml.Matrix4f;

/** Folded assembly rises together, slides back, then unfolds past the hand. */
public final class MantisArticulation {
  public static final float SUPPORT_LENGTH = .405f;

  public static Matrix4f mount(float open) {
    return mount(open, .25f);
  }

  public static Matrix4f mount(float open, float armDepth) {
    float lift = open;
    return new Matrix4f().translate(0, .43f, -armDepth / 2 + .060f - .105f * lift);
  }

  public static Matrix4f support(float hinge) {
    return support(hinge, hinge);
  }

  public static Matrix4f carriage(float slide) {
    return new Matrix4f().translate(0, -.06f * slide, 0);
  }

  public static Matrix4f support(float rise, float unfold) {
    return new Matrix4f().rotateX((float) Math.toRadians(-180 + 55 * rise - 20 * unfold));
  }

  public static Matrix4f blade(float hinge, float extension) {
    float support = -180 + 55 * hinge - 20 * extension;
    float absolute = 55 * hinge * (1 - extension) - 8 * extension;
    return new Matrix4f()
        .translate(0, SUPPORT_LENGTH, 0)
        .rotateX((float) Math.toRadians(absolute - support));
  }

  public static Matrix4f tip(float extension) {
    return new Matrix4f()
        .translate(
            0, .0162f + .2764125f * MantisMotion.smooth(Math.clamp(extension / .7f, 0, 1)), 0);
  }

  public static Matrix4f edge(float extension) {
    return new Matrix4f()
        .translate(
            0,
            .0081f + .2764125f * MantisMotion.smooth(Math.clamp((extension - .3f) / .7f, 0, 1)),
            0);
  }

  private MantisArticulation() {}
}
