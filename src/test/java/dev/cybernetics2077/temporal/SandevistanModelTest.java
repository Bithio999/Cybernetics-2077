package dev.cybernetics2077.temporal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SandevistanModelTest {
  @Test
  void apogeeHasDistinctStrengthAndEqualUserParity() {
    double a = TemporalRules.acceleration(SandevistanModel.APOGEE.slowing(5));
    double d = TemporalRules.acceleration(SandevistanModel.DYNALAR.slowing(5));
    assertEquals(1.25, TemporalRules.pace(a, a), 1e-9);
    assertEquals(.1875, TemporalRules.pace(1, a), 1e-9);
    assertEquals(.375, TemporalRules.pace(d, a), 1e-9);
    assertEquals(.15, TemporalRules.pace(1, a) / TemporalRules.pace(a, a), 1e-9);
  }

  @Test
  void batteryAndBonusesAreModelSpecific() {
    assertEquals(6, SandevistanModel.APOGEE.duration(5));
    assertEquals(25, SandevistanModel.APOGEE.recharge(5));
    assertEquals(.2f, SandevistanModel.APOGEE.critChance(5));
    assertEquals(.2f, SandevistanModel.APOGEE.critDamage(5));
    assertEquals(1. / 120, SandevistanModel.APOGEE.minimumCharge(5), 1e-9);
    assertEquals(.2, SandevistanModel.DYNALAR.minimumCharge(5), 1e-9);
  }

  @Test
  void allExistingDynalarProfilesRemainUnchanged() {
    for (int t = 2; t <= 5; t++) {
      assertEquals(TemporalRules.slowing(t), SandevistanModel.DYNALAR.slowing(t));
      assertEquals(TemporalRules.duration(t), SandevistanModel.DYNALAR.duration(t));
      assertEquals(TemporalRules.recharge(t), SandevistanModel.DYNALAR.recharge(t));
    }
    assertEquals(SandevistanModel.DYNALAR, SandevistanModel.of(false));
  }
}
