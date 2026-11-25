package dev.cybernetics2077.client.temporal;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** Saved world positions use the current camera view; saved arms also undo their birth camera. */
public final class TrailTransform {
  public static Matrix4f modelView(
      Matrix4f view, double x, double y, double z, Quaternionf birthCamera) {
    var result = new Matrix4f(view).translate((float) x, (float) y, (float) z);
    if (birthCamera != null) result.rotate(birthCamera);
    return result;
  }

  private TrailTransform() {}
}
