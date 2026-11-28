package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AnimationLatticeTest {
  @Test
  void localPhaseIsContinuousAndRateIsModelSpecific() {
    for (double pace : new double[] {.00125, .025, .625, .1875}) {
      var grid = new AnimationLattice();
      grid.touch(0, 0, 0, 10, pace);
      var c = grid.clocks.get(new AnimationLattice.Node(0, 0, 0));
      assertEquals(0, c.lag);
      for (int t = 11; t <= 110; t++) {
        grid.touch(0, 0, 0, t, pace);
        grid.finish(t);
      }
      assertEquals(100 * (1 - pace), c.lag, 1e-8);
      double local = 110 - c.lag;
      grid.finish(111); // Consume the final old-rate interval and leave the field.
      double next = 111 - c.lag;
      assertEquals(pace, next - local, 1e-8);
      grid.finish(112);
      assertEquals(1, 112 - c.lag - next, 1e-8);
    }
  }

  @Test
  void leavingDoesNotDiscardPhaseAndRestorationFadesBeforePruning() {
    var grid = new AnimationLattice();
    grid.touch(-3, 2, 5, 0, .1875);
    grid.touch(-3, 2, 5, 20, .1875);
    var c = grid.clocks.get(new AnimationLattice.Node(-3, 2, 5));
    grid.finish(21);
    double lag = c.lag;
    grid.finish(80);
    assertEquals(lag, c.lag);
    assertEquals(1, c.weight(80));
    assertEquals(.5, c.weight(100), 1e-8);
    assertEquals(0, c.weight(120));
    grid.finish(121);
    assertTrue(grid.clocks.isEmpty());
  }

  @Test
  void normalWorldCreatesNoClocksAndCacheIsBounded() {
    var grid = new AnimationLattice();
    grid.touch(0, 0, 0, 0, 1);
    assertTrue(grid.clocks.isEmpty());
    for (int i = 0; i < AnimationLattice.LIMIT + 50; i++) grid.touch(i, 0, 0, 0, .625);
    assertEquals(AnimationLattice.LIMIT, grid.clocks.size());
  }

  @Test
  void shaderHashUsesIdenticalOverflowAndSupportsNegativeWorldCoordinates() {
    for (int x : new int[] {-7500000, -1, 0, 1, 7500000})
      for (int y : new int[] {-16, 0, 80})
        for (int z : new int[] {-7500000, 0, 7500000}) {
          long hash =
              (Integer.toUnsignedLong(x) * 73856093L
                      ^ Integer.toUnsignedLong(y) * 19349663L
                      ^ Integer.toUnsignedLong(z) * 83492791L)
                  & (AnimationLattice.SLOTS - 1);
          assertEquals((int) hash, AnimationLattice.hash(x, y, z));
        }
  }
}
