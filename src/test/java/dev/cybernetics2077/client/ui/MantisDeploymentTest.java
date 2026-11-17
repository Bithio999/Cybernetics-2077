package dev.cybernetics2077.client.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.mantis.MantisDeployment;
import org.junit.jupiter.api.Test;

final class MantisDeploymentTest {
  @Test
  void latePacketCannotSkipTheExistingHandsStartingPose() {
    var a = new MantisDeployment(0, 1);
    assertEquals(0, a.sample(1000));
    assertEquals(.5f, a.sample(1015), .00001);
    assertEquals(1, a.sample(1030));
    assertEquals(1, a.sample(1040));
  }

  @Test
  void reversingKeepsTheLastActuallyDisplayedPose() {
    var a = new MantisDeployment(0, 1);
    a.sample(100);
    a.sample(112);
    var b = new MantisDeployment(a.last(), 0);
    assertEquals(a.last(), b.sample(900), .00001);
    assertTrue(b.visible());
    assertEquals(0, b.sample(913), .00001);
    assertFalse(b.visible());
  }

  @Test
  void joiningAndPausedFramesDoNotCreateFalseTransitions() {
    var a = new MantisDeployment(1, 1);
    assertEquals(1, a.sample(0));
    var b = new MantisDeployment(0, 1);
    b.sample(10);
    float p = b.sample(15);
    for (int i = 0; i < 100; i++) assertEquals(p, b.sample(15));
  }
}
