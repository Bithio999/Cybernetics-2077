package dev.cybernetics2077.client.mantis;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** First-person handover starts at Minecraft's actual empty-hand matrix, not a substitute pose. */
public final class MantisView {
  public static final float WEAPON_FOV = 68;

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
            .translate(side * (main ? .59f : .43f), main ? -.72f : -.68f, main ? -.73f : -.78f)
            .rotateZ((float) Math.PI)
            .scale(.9f)
            .mul(shoulder(left, slim, pose));
    if (blend <= 0) return start;
    var view = anchor(blend(start, goal, blend), slim);
    // Apply the small ready-pose drop after the shoulder guard so it is not cancelled by it.
    return view.m31(view.m31() - .025f * blend);
  }

  /** Camera-space stance is independent of the world-space third-person shoulders. */
  private static Matrix4f shoulder(boolean left, boolean slim, MantisMotion.Pose pose) {
    float side = left ? -1 : 1, ready = pose.ready();
    return new Matrix4f()
        .rotateZ(rad(-side * 6 * ready + pose.roll()))
        .rotateY(rad(-side * 12 * ready + pose.yaw()))
        .rotateX(rad(-101 * ready + pose.pitch()))
        // Present the dorsal forearm and mount upward.
        .rotateY(rad(side * 28 * ready))
        .rotateY(rad((left ? -90 : 90) * (1 - ready)))
        .translate(
            MantisRig.nativeToMount(left)
                .transformDirection(new Vector3f(MantisRig.centerOffset(left, slim), 0, 0)));
  }

  /** Keep the finite shoulder caps below the view without modifying any skin geometry. */
  private static Matrix4f anchor(Matrix4f frame, boolean slim) {
    float depth = slim ? 3 / 16f : 4 / 16f, sleeve = .25f / 16;
    float tangent = (float) Math.tan(Math.toRadians(Math.max(70, WEAPON_FOV)) / 2);
    float capY = -.125f - sleeve;
    float centre =
        (frame.m11() - tangent * frame.m12()) * capY + frame.m31() - tangent * frame.m32() + .02f;
    float across = (frame.m01() - tangent * frame.m02()) * (.125f + sleeve);
    float deep = (frame.m21() - tangent * frame.m22()) * (depth / 2 + sleeve);
    // Smooth support of the cap rectangle against the bottom frustum plane.
    // This is a smooth maximum of its four corners, with no per-frame allocations.
    float boundary = centre + softAbs(across) + softAbs(deep);
    float drop = .5f * (boundary + (float) Math.sqrt(boundary * boundary + .000001f));
    return frame.m31(frame.m31() - drop);
  }

  private static float softAbs(float value) {
    float absolute = Math.abs(value);
    return absolute + .008f * (float) Math.log1p(Math.exp(-2 * absolute / .008));
  }

  /** Dedicated weapon lens; never modifies the player's world FOV setting. */
  public static Matrix4f projection(Matrix4fc current, float ready) {
    var result = new Matrix4f(current);
    float focal = Math.abs(current.m11());
    if (ready <= 0 || !Float.isFinite(focal) || focal < .0001f) return result;
    float target = 1f / (float) Math.tan(Math.toRadians(WEAPON_FOV) / 2);
    float ratio = (focal + (target - focal) * Math.clamp(ready, 0, 1)) / focal;
    return result.m00(current.m00() * ratio).m11(current.m11() * ratio);
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
