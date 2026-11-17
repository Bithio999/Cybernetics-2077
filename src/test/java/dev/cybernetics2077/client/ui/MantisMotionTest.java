package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.MantisMotion;
import dev.cybernetics2077.combat.CombatRules;
import org.junit.jupiter.api.Test;

final class MantisMotionTest {
  @Test
  void panelsOpenBeforeBladeAndRetractionReturnsToClosed() {
    var beginning = MantisMotion.sample(true, 0, 100, 0, false, 0, false);
    var opened = MantisMotion.sample(true, 24, 100, 0, false, 0, false);
    var closed = MantisMotion.sample(false, 24, 100, 0, false, 0, false);
    assertEquals(0, beginning.panels());
    assertEquals(0, beginning.blade());
    assertEquals(1, opened.panels());
    assertEquals(1, opened.blade());
    assertEquals(0, closed.panels());
    assertEquals(0, closed.blade());
    for (float t = 0; t < 24; t += .05f) {
      var pose = MantisMotion.sample(true, t, 100, 0, false, 0, false);
      assertTrue(pose.panels() >= pose.blade());
    }
  }

  @Test
  void strikesAreContinuousFiniteAndAlternateArms() {
    for (int combo = 0; combo < 4; combo++)
      for (boolean left : new boolean[] {false, true}) {
        var previous = MantisMotion.sample(true, 100, 0, combo, false, 0, left);
        for (float t = .02f; t <= 24; t += .02f) {
          var pose = MantisMotion.sample(true, 100, t, combo, false, 0, left);
          assertTrue(Float.isFinite(pose.roll()) && Float.isFinite(pose.pitch()));
          assertTrue(Math.abs(pose.roll() - previous.roll()) < 2);
          assertTrue(Math.abs(pose.x() - previous.x()) < .02);
          previous = pose;
        }
      }
    assertNotEquals(0, MantisMotion.sample(true, 100, 4, 0, false, 0, false).roll());
    assertEquals(0, MantisMotion.sample(true, 100, 4, 0, false, 0, true).roll());
    assertNotEquals(0, MantisMotion.sample(true, 100, 4, 1, false, 0, true).roll());
  }

  @Test
  void contactFramesFallInsideTheFastStroke() {
    for (int combo = 0; combo < 4; combo++) {
      var pose =
          MantisMotion.sample(
              true, 100, CombatRules.impactTick(combo), combo, false, 0, combo == 1);
      assertTrue(pose.trail() > .5);
      assertTrue(CombatRules.damage(combo) > 0);
    }
    assertFalse(CombatRules.ready(9, 0, 10));
    assertTrue(CombatRules.ready(10, 0, 10));
  }

  @Test
  void rotatedTabKeepsItsStrokeWidthAtEveryGuiScale() {
    for (float scale : new float[] {.25f, .5f, 1, 2, 3})
      for (float angle : new float[] {0, 45, 90, 180, 270}) {
        var pose = new org.joml.Matrix4f().scale(scale).rotateZ((float) Math.toRadians(angle));
        assertEquals(scale, Geometry.poseScale(pose), .00001);
      }
  }
}
