package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class FlightMathTest {
  @Test
  void fractionalStepsRecoverNativeDisplacementAndForces() {
    for (double q : new double[] {.6, .97, .98, 1, 1 - 1e-7})
      for (int pieces : new int[] {2, 16, 1000}) {
        double x = 12, y = -.4, z = 21, px = 0, py = 0, pz = 0, dt = 1.0 / pieces, g = .15;
        for (int i = 0; i < pieces; i++) {
          var d = FlightMath.displacement(x, y, z, q, g, dt);
          px += d.x();
          py += d.y();
          pz += d.z();
          double drag = FlightMath.drag(q, dt);
          x *= drag;
          y = y * drag - g * FlightMath.sum(q, dt);
          z *= drag;
        }
        assertEquals(12, px, 1e-8);
        assertEquals(-.4, py, 1e-8);
        assertEquals(21, pz, 1e-8);
        assertEquals(12 * q, x, 1e-8);
        assertEquals(-.4 * q - g, y, 1e-8);
        assertEquals(21 * q, z, 1e-8);
      }
  }

  @Test
  void nativeFullStepIsUnchanged() {
    assertEquals(new FlightMath.Vector(1, 2, 3), FlightMath.displacement(1, 2, 3, .98, .15, 1));
    assertEquals(1, FlightMath.sum(.98, 1), 1e-12);
  }

  @Test
  void onlyMechanicalClipsUseWeaponTime() {
    for (String n : new String[] {"shoot", "shoot_empty", "bolt", "pump_action", "fire_auto"})
      assertTrue(FlightMath.mechanism(n), n);
    for (String n :
        new String[] {
          "reload_empty",
          "reload_bolt",
          "reload_tactical_xmag",
          "static_bolt_caught",
          "inspect_empty_pump",
          "switch_pump",
          "draw_bolt",
          "idle",
          "walk",
          "inspect",
          "aim"
        }) assertFalse(FlightMath.mechanism(n), n);
  }
}
