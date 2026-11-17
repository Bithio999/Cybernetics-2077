package dev.cybernetics2077.client.mantis;

import org.joml.Matrix4f;

/** Canonical player-model axes: X left, Y down, Z backward. Every rotation has a real joint. */
public final class MantisRig {
  public static final float ELBOW_Y = .25f;

  public static float centerOffset(boolean left, boolean slim) {
    return (left ? 1 : -1) * (slim ? .5f : 1f) / 16f;
  }

  public static Matrix4f shoulder(boolean left, boolean slim, MantisMotion.Pose motion) {
    float side = left ? -1 : 1;
    return new Matrix4f()
        .rotateZ((float) Math.toRadians(-side * 8 * motion.ready() + motion.roll()))
        .rotateY((float) Math.toRadians(-side * 25 * motion.ready() + motion.yaw()))
        .rotateX((float) Math.toRadians(-67 * motion.ready() + motion.pitch()))
        // This offset belongs AFTER shoulder rotation. Moving the pivot to the arm centre
        // disconnects its inner edge from the torso during a large slash.
        .translate(centerOffset(left, slim), 0, 0);
  }

  public static Matrix4f elbow(MantisMotion.Pose motion) {
    return new Matrix4f()
        .translate(0, ELBOW_Y, 0)
        .rotateX((float) Math.toRadians(motion.elbow()))
        .rotateY((float) Math.toRadians(motion.twist()))
        .translate(0, -ELBOW_Y, 0);
  }

  private MantisRig() {}
}
