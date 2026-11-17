package dev.cybernetics2077.client.mantis;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** First-person handover starts at Minecraft's actual empty-hand matrix, not a substitute pose. */
public final class MantisView {
  private static float rad(float degrees) {
    return (float) Math.toRadians(degrees);
  }

  public static Matrix4f vanilla(boolean left, boolean slim, float equip, float swing) {
    float side = left ? -1 : 1, root = (float) Math.sqrt(swing);
    return new Matrix4f()
        .translate(
            side * (-.3f * (float) Math.sin(root * Math.PI) + .64000005f),
            .4f * (float) Math.sin(root * Math.PI * 2) - .6f - equip * .6f,
            -.4f * (float) Math.sin(swing * Math.PI) - .71999997f)
        .rotateY(rad(side * 45))
        .rotateY(rad(side * (float) Math.sin(root * Math.PI) * 70))
        .rotateZ(rad(side * (float) Math.sin(swing * swing * Math.PI) * -20))
        .translate(-side, 3.6f, 3.5f)
        .rotateZ(rad(side * 120))
        .rotateX(rad(200))
        .rotateY(rad(-side * 135))
        .translate(side * 5.6f, 0, 0)
        .translate(-side * 5 / 16f, (slim ? 2.5f : 2f) / 16f, 0)
        .rotateZ(side * .1f)
        .translate(MantisRig.centerOffset(left, slim), 0, 0)
        .mul(MantisRig.mountToNative(left));
  }

  public static Matrix4f frame(
      boolean left, boolean main, boolean slim, float equip, float swing, MantisMotion.Pose pose) {
    float side = left ? -1 : 1, blend = pose.ready();
    var start = vanilla(left, slim, equip, swing);
    if (!main) {
      var t = start.getTranslation(new Vector3f());
      start.setTranslation(t.x + side * 1.25f, t.y - .85f, t.z);
    }
    var goal =
        new Matrix4f()
            .translate(side * .72f, -.35f, -.65f)
            .rotateZ((float) Math.PI)
            .scale(.9f)
            .mul(MantisRig.shoulder(left, slim, pose));
    return blend(start, goal, blend);
  }

  public static Matrix4f blend(Matrix4f a, Matrix4f b, float t) {
    if (t <= 0) return new Matrix4f(a);
    if (t >= 1) return new Matrix4f(b);
    var position = a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()), t);
    var rotation =
        a.getUnnormalizedRotation(new Quaternionf())
            .slerp(b.getUnnormalizedRotation(new Quaternionf()), t);
    var scale = a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()), t);
    return new Matrix4f().translationRotateScale(position, rotation, scale);
  }

  private MantisView() {}
}
