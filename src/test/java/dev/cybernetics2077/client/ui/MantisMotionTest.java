package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.MantisMotion;
import dev.cybernetics2077.combat.CombatRules;
import org.junit.jupiter.api.Test;

final class MantisMotionTest {
  @Test
  void beginningChargeDuringARecoveringCutDoesNotResetTheHandPosition() {
    for (boolean left : new boolean[] {false, true}) {
      var before = MantisMotion.sample(true, 100, 6.999f, 0, true, 1.999f, left);
      var after = MantisMotion.sample(true, 100, 7.001f, 0, true, 2.001f, left);
      assertEquals(before.x(), after.x(), .001f);
      assertEquals(before.y(), after.y(), .001f);
      assertEquals(before.z(), after.z(), .001f);
    }
  }

  @Test
  void eachCutRetractsForWindupAndExtendsAtItsDamageFrame() {
    for (int combo = 0; combo < 3; combo++) {
      boolean left = combo == 1;
      var wind = MantisMotion.sample(true, 100, 1.6f, combo, false, 0, left);
      var contact = MantisMotion.sample(true, 100, 4, combo, false, 0, left);
      var follow = MantisMotion.sample(true, 100, 5.8f, combo, false, 0, left);
      var rest = MantisMotion.sample(true, 100, 10, combo, false, 0, left);
      assertTrue(wind.tip() < .65f);
      assertEquals(1, contact.tip(), .00001);
      assertTrue(follow.tip() < .85f);
      assertEquals(1, rest.tip(), .00001);
      assertEquals(1, contact.extension(), .00001, "Only the telescoping leaves move during a cut");
      var before = MantisMotion.sample(true, 100, 3.99f, combo, false, 0, left);
      var after = MantisMotion.sample(true, 100, 4.01f, combo, false, 0, left);
      float incoming = (contact.x() - before.x()) / .01f;
      float outgoing = (after.x() - contact.x()) / .01f;
      assertTrue(Math.abs(incoming) > .01f, "The cut must flow through contact");
      assertEquals(incoming, outgoing, .003f);
    }
  }

  @Test
  void lungeReelsBackThenDrivesBothArmsForwardWithExtendedBlades() {
    for (boolean left : new boolean[] {false, true}) {
      var held = MantisMotion.sample(true, 100, 100, 3, true, 8, left);
      var flying = MantisMotion.sample(true, 100, 1, 3, false, 0, left, 0, 0, false);
      var arrived = MantisMotion.sample(true, 100, 10, 3, false, 0, left, 0, 1, false);
      var contact = MantisMotion.sample(true, 100, 7, 3, false, 0, left);
      assertTrue(held.z() > .2f && held.tip() < .35f);
      assertTrue(Math.abs(held.roll()) < 5, "Windup is a pullback, not an arm spin");
      assertEquals(held.z(), flying.z(), .00001);
      assertTrue(arrived.z() < -.15f && arrived.tip() > .99f);
      assertEquals(arrived.z(), contact.z(), .00001);
      assertEquals(arrived.pitch(), contact.pitch(), .00001);
      assertEquals(arrived.elbow(), contact.elbow(), .00001);
      assertEquals(arrived.tip(), contact.tip(), .00001);
      assertEquals(0, MantisMotion.sample(true, 100, 22, 3, false, 0, left).z(), .00001);
    }
  }

  @Test
  void idleBreathingIsSmallContinuousAndFadesOutDuringActions() {
    var rest = MantisMotion.sample(true, 100, 100, 0, false, 0, false, 0, -1, false);
    var breathing = MantisMotion.sample(true, 100, 100, 0, false, 0, false, 20, -1, false);
    assertNotEquals(rest.y(), breathing.y());
    for (float clock = 0; clock < 300; clock += .25f) {
      var pose = MantisMotion.sample(true, 100, 100, 0, false, 0, false, clock, -1, false);
      assertTrue(Math.abs(pose.x()) <= .00301 && Math.abs(pose.y()) <= .00601);
      assertTrue(Math.abs(pose.z()) <= .00301 && Math.abs(pose.roll()) < .5);
    }
    assertEquals(
        MantisMotion.sample(true, 100, 4, 0, false, 0, false),
        MantisMotion.sample(true, 100, 4, 0, false, 0, false, 20, -1, false));
    assertEquals(
        MantisMotion.sample(true, 100, 100, 3, true, 8, false),
        MantisMotion.sample(true, 100, 100, 3, true, 8, false, 20, -1, false));
  }

  @Test
  void leftHandedCutsMirrorTheRightHandedChoreography() {
    for (int combo = 0; combo < 3; combo++) {
      var right = MantisMotion.sample(true, 100, 4, combo, false, 0, combo == 1, 0, -1, false);
      var left = MantisMotion.sample(true, 100, 4, combo, false, 0, combo != 1, 0, -1, true);
      assertEquals(-right.x(), left.x(), .00001);
      assertEquals(-right.roll(), left.roll(), .00001);
      assertEquals(-right.yaw(), left.yaw(), .00001);
      assertEquals(right.y(), left.y());
      assertEquals(right.z(), left.z());
      assertEquals(right.tip(), left.tip());
    }
  }

  @Test
  void panelsOpenBeforeBladeAndRetractionReturnsToClosed() {
    var beginning = MantisMotion.sample(true, 0, 100, 0, false, 0, false);
    var opened = MantisMotion.sample(true, CombatRules.DEPLOY, 100, 0, false, 0, false);
    var closed = MantisMotion.sample(false, CombatRules.DEPLOY, 100, 0, false, 0, false);
    assertEquals(0, beginning.panels());
    assertEquals(0, beginning.blade());
    assertEquals(1, opened.panels());
    assertEquals(1, opened.blade());
    assertEquals(0, closed.panels());
    assertEquals(0, closed.blade());
    for (float t = 0; t < CombatRules.DEPLOY; t += .05f) {
      var pose = MantisMotion.sample(true, t, 100, 0, false, 0, false);
      assertTrue(pose.panels() >= pose.blade());
      if (pose.blade() > 0 && pose.blade() < 1) {
        assertEquals(0, pose.slide());
        assertEquals(0, pose.extension());
      }
    }
  }

  @Test
  void carriageExtendsAfterHingeAndRetractsBeforeHatchesClose() {
    for (float age = 0; age <= CombatRules.DEPLOY; age += .125f) {
      var a = MantisMotion.sample(true, age, 100, 0, false, 0, false);
      var b = MantisMotion.sample(false, CombatRules.DEPLOY - age, 100, 0, false, 0, false);
      assertEquals(a.panels(), b.panels(), .00001);
      assertEquals(a.blade(), b.blade(), .00001);
      assertEquals(a.extension(), b.extension(), .00001);
      assertEquals(a.tip(), b.tip(), .00001);
      assertEquals(a.slide(), b.slide(), .00001);
      assertTrue(a.blade() >= a.slide() && a.slide() >= a.extension());
      assertTrue(a.extension() >= a.tip());
      assertTrue(a.panels() >= a.blade());
      assertTrue(a.blade() >= a.extension());
      assertTrue(a.elbow() >= -65 && a.elbow() <= 0);
    }
    var emerging = MantisMotion.sample(true, 8, 100, 0, false, 0, false);
    assertTrue(emerging.blade() > 0 && emerging.panels() < 1);
    assertEquals(0, MantisMotion.sample(true, 8, 100, 0, false, 0, false).extension());
    assertEquals(1, MantisMotion.sample(true, 30, 100, 0, false, 0, false).extension());
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
    assertEquals(0, MantisMotion.sample(true, 100, 4, 0, false, 0, true).roll(), .00001);
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
