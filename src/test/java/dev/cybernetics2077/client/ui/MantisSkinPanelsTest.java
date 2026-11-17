package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class MantisSkinPanelsTest {
  @Test
  void closedPlatesTileTheDorsalSkinWithoutMissingOrDuplicatedAreas() {
    for (float width : new float[] {3 / 16f, 4 / 16f}) {
      float border = MantisRig.ARM_WIDTH * MantisSkinPanels.WIDTH;
      for (int i = 0; i < 41; i++)
        for (int j = 0; j < 71; j++) {
          float x = -border + 2 * border * (i + .371f) / 41;
          float y =
              MantisSkinPanels.START
                  + (MantisSkinPanels.END - MantisSkinPanels.START) * (j + .271f) / 71;
          int count = 0;
          for (var panel : MantisSkinPanels.ALL)
            if (panel != MantisSkinPanels.Panel.SIDE_LEFT
                && panel != MantisSkinPanels.Panel.SIDE_RIGHT
                && MantisSkinPanels.region(panel, width).contains(x, y, -width / 2)) count++;
          assertEquals(1, count, "Overlapping or missing dermal plate");
        }
    }
  }

  @Test
  void platesStartAtNativeSkinAndFollowContinuousSeparateTracks() {
    for (float width : new float[] {3 / 16f, 4 / 16f})
      for (boolean sleeve : new boolean[] {false, true})
        for (var panel : MantisSkinPanels.ALL) {
          assertTrue(
              new Matrix4f().equals(MantisSkinPanels.transform(panel, width, 0, sleeve), .000001f));
          var previous =
              MantisSkinPanels.transform(panel, width, 0, sleeve)
                  .transformPosition(new Vector3f(0, .3f, -width / 2));
          for (float t = .001f; t <= 1; t += .001f) {
            var matrix = MantisSkinPanels.transform(panel, width, t, sleeve);
            assertEquals(1, matrix.determinant(), .00001);
            var point = matrix.transformPosition(new Vector3f(0, .3f, -width / 2));
            assertTrue(previous.distance(point) < .01, "Skin cover jumped during deployment");
            previous = point;
          }
        }
    var a = MantisSkinPanels.transform(MantisSkinPanels.Panel.MID_LEFT, .25f, .65f, false);
    var b = MantisSkinPanels.transform(MantisSkinPanels.Panel.CARRIER_LEFT, .25f, .65f, false);
    var c = MantisSkinPanels.transform(MantisSkinPanels.Panel.WRIST, .25f, .65f, false);
    assertFalse(a.equals(b, .01f));
    assertFalse(a.equals(c, .01f));
  }

  @Test
  void deployedCoversMoveAwayFromTheCentralMechanismAndKeepThePalmClosed() {
    for (float width : new float[] {3 / 16f, 4 / 16f}) {
      var wrist =
          MantisSkinPanels.transform(MantisSkinPanels.Panel.WRIST, width, 1, false)
              .transformPosition(new Vector3f(0, .44f, -width / 2));
      assertTrue(wrist.y > .53f, "Wrist collar did not clear the mount");
      var rear =
          MantisSkinPanels.transform(MantisSkinPanels.Panel.PROXIMAL, width, 1, false)
              .transformPosition(new Vector3f(0, .1f, -width / 2));
      assertTrue(rear.y < 0, "Proximal cover must slide behind the emerging support");
      for (var side :
          new MantisSkinPanels.Panel[] {
            MantisSkinPanels.Panel.SIDE_LEFT, MantisSkinPanels.Panel.SIDE_RIGHT
          }) {
        var r = MantisSkinPanels.region(side, width);
        assertFalse(
            r.contains(MantisRig.ARM_WIDTH / 2, .3f, .08f), "A side cover cuts into the palm");
      }
    }
  }
}
