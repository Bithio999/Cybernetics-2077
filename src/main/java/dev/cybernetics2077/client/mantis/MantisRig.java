package dev.cybernetics2077.client.mantis;

import org.joml.Matrix4f;

/** Mount-local axes: X across the hand, +Y toward fingers, -Z dorsal. Native skin is rebased. */
public final class MantisRig {
  // Vanilla has one rigid arm; this is our cyberarm joint, not a vanilla anatomical boundary.
  public static final float ELBOW_Y = .10f;
  public static final float ARM_WIDTH = .25f;

  /** Native hanging arms have their knuckle face on the outward X side, not front Z. */
  public static Matrix4f nativeToMount(boolean left) {
    return new Matrix4f().rotateY((float) Math.toRadians(left ? 90 : -90));
  }

  public static Matrix4f mountToNative(boolean left) {
    return new Matrix4f().rotateY((float) Math.toRadians(left ? -90 : 90));
  }

  public static org.joml.Vector3f shoulderSocket(boolean left, boolean slim) {
    return nativeToMount(left)
        .transformPosition(new org.joml.Vector3f(-centerOffset(left, slim), 0, 0));
  }

  public static float centerOffset(boolean left, boolean slim) {
    return (left ? 1 : -1) * (slim ? .5f : 1f) / 16f;
  }

  public static Matrix4f shoulder(boolean left, boolean slim, MantisMotion.Pose motion) {
    float side = left ? -1 : 1;
    return new Matrix4f()
        .rotateZ((float) Math.toRadians(side * 10 * motion.ready() - motion.roll()))
        .rotateY((float) Math.toRadians(side * 12 * motion.ready() + motion.yaw()))
        .rotateX((float) Math.toRadians(-60 * motion.ready() + motion.pitch()))
        .rotateY((float) Math.toRadians(side * 40 * motion.ready()))
        // Rebase the native pivot offset together with the skin. Keep this after
        // shoulder rotation so the arm stays attached throughout a slash.
        .rotateY((float) Math.toRadians((left ? -90 : 90) * (1 - motion.ready())))
        .translate(
            nativeToMount(left)
                .transformDirection(new org.joml.Vector3f(centerOffset(left, slim), 0, 0)));
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
