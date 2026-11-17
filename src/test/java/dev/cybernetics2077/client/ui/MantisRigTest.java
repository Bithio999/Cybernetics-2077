package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.*;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class MantisRigTest {
  @Test
  void shoulderSocketNeverLeavesItsParentAndBothSegmentsShareTheElbow() {
    for (boolean left : new boolean[] {false, true})
      for (boolean slim : new boolean[] {false, true})
        for (int combo = 0; combo < 4; combo++)
          for (float t = 0; t < 24; t += .125f) {
            var motion = MantisMotion.sample(true, 100, t, combo, false, 0, left);
            var shoulder = MantisRig.shoulder(left, slim, motion);
            var socket =
                shoulder.transformPosition(new Vector3f(-MantisRig.centerOffset(left, slim), 0, 0));
            assertEquals(0, socket.length(), .00001, "Shoulder detached");
            var upperEnd = shoulder.transformPosition(new Vector3f(0, MantisRig.ELBOW_Y, 0));
            var lowerStart =
                new org.joml.Matrix4f(shoulder)
                    .mul(MantisRig.elbow(motion))
                    .transformPosition(new Vector3f(0, MantisRig.ELBOW_Y, 0));
            assertEquals(0, upperEnd.distance(lowerStart), .00001, "Elbow disconnected");
            assertEquals(0, motion.x());
            assertEquals(0, motion.y());
            assertEquals(0, motion.z());
          }
  }

  @Test
  void chargingKeepsTheSameAttachmentPoints() {
    for (float age = 0; age < 25; age += .25f) {
      var p = MantisMotion.sample(true, 100, 100, 0, true, age, false);
      var m = MantisRig.shoulder(false, false, p);
      var a = m.transformPosition(new Vector3f(0, MantisRig.ELBOW_Y, 0));
      var b =
          new org.joml.Matrix4f(m)
              .mul(MantisRig.elbow(p))
              .transformPosition(new Vector3f(0, MantisRig.ELBOW_Y, 0));
      assertTrue(a.distance(b) < .00001);
    }
  }
}
