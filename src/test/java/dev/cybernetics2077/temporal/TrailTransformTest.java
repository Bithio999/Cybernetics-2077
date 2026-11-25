package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.temporal.TrailTransform;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

final class TrailTransformTest {
  @Test
  void savedBodyStaysCentredWhenObservedFromEveryCompassDirectionAndPitch() {
    var projection = new Matrix4f().perspective((float) Math.toRadians(70), 16f / 9, .05f, 1000);
    for (double yaw : new double[] {0, 45, 90, 135, 180, 225, 270, 315})
      for (double pitch : new double[] {-30, 0, 35}) {
        double a = Math.toRadians(yaw), b = Math.toRadians(pitch);
        var direction =
            new Vector3f(
                (float) (Math.sin(a) * Math.cos(b)),
                (float) Math.sin(b),
                (float) (-Math.cos(a) * Math.cos(b)));
        var view = new Matrix4f().lookAt(new Vector3f(), direction, new Vector3f(0, 1, 0));
        var position = new Vector3f(direction).mul(8);
        var point =
            new Vector4f(0, 0, 0, 1)
                .mul(TrailTransform.modelView(view, position.x, position.y, position.z, null))
                .mul(projection);
        assertTrue(point.w > 0, "Saved pose ended up behind the camera");
        assertEquals(0, point.x / point.w, .00001);
        assertEquals(0, point.y / point.w, .00001);
      }
  }

  @Test
  void turningTheCameraDoesNotDragASavedFirstPersonHandWithIt() {
    var birth = new Quaternionf(); // Captured looking toward -Z.
    var currentView = new Matrix4f().lookAt(0, 0, 0, -1, 0, 0, 0, 1, 0);
    var point =
        TrailTransform.modelView(currentView, 0, 0, 0, birth)
            .transformPosition(new Vector3f(0, 0, -1));
    assertEquals(1, point.x, .00001);
    assertEquals(0, point.z, .00001);
    assertEquals(0, birth.x);
    assertEquals(1, birth.w); // Do not mutate the saved camera.
  }

  @Test
  void savedHandStaysAtItsWorldPositionWhenTheCameraMoves() {
    var birth = new Quaternionf().rotationY((float) (Math.PI / 2));
    // Local (0,0,-2) at birth becomes world (-2,0,0), captured camera was at (10,3,5).
    var view = new Matrix4f().lookAt(0, 0, 0, -1, 0, 0, 0, 1, 0);
    var model = TrailTransform.modelView(view, 1, 1, 0, birth); // Current camera (9,2,5).
    var point = model.transformPosition(new Vector3f(0, 0, -2));
    assertEquals(0, point.x, .00001);
    assertEquals(1, point.y, .00001);
    assertEquals(-1, point.z, .00001);
  }
}
