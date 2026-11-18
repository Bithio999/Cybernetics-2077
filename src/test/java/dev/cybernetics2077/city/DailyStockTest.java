package dev.cybernetics2077.city;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

final class DailyStockTest {
  private static int[] catalogue() {
    var tiers = new ArrayList<Integer>();
    for (int tier = 1; tier <= 5; tier++)
      for (int item = 0; item < (tier == 1 ? 7 : 17); item++) tiers.add(tier);
    return tiers.stream().mapToInt(Integer::intValue).toArray();
  }

  @Test
  void starterOpticsAlwaysAvailableAtZeroThroughTenAndHigherTiersStayHidden() {
    int[] tiers = catalogue();
    for (int cred = 0; cred <= 50; cred++)
      for (int day = 0; day < 100; day++) {
        var selected = DailyStock.select(tiers, 27341, day, cred, 0);
        if (cred <= 10) assertTrue(selected.get(0), "Missing starter optics at cred " + cred);
        assertTrue(selected.cardinality() > 0 && selected.cardinality() <= 10);
        final int reputation = cred;
        selected.stream().forEach(i -> assertTrue((tiers[i] - 1) * 10 <= reputation));
      }
  }

  @Test
  void reopenAndReloadDoNotRerollButEveryDawnChangesSelection() {
    int[] tiers = catalogue();
    for (int cred : new int[] {0, 5, 10, 19, 20, 30, 40, 50})
      for (long day = -1; day < 150; day++) {
        var first = DailyStock.select(tiers, 391, day, cred, 0);
        assertEquals(first, DailyStock.select(tiers, 391, day, cred, 0));
        assertNotEquals(first, DailyStock.select(tiers, 391, day + 1, cred, 0));
      }
    assertNotEquals(
        DailyStock.select(tiers, 1, 10, 50, 0),
        DailyStock.select(tiers, 2, 10, 50, 0),
        "Every doctor had the same stock");
  }

  @Test
  void actualAssortmentsImproveAsReputationRisesWithinEachUnlockBand() {
    int[] tiers = catalogue();
    for (int tier = 2; tier <= 5; tier++) {
      double atUnlock = appearances(tiers, tier, (tier - 1) * 10);
      double later = appearances(tiers, tier, (tier - 1) * 10 + 9);
      assertTrue(atUnlock > 0, "New tier never appears");
      assertTrue(
          later > atUnlock * 1.5,
          "Tier " + tier + " did not become more common: " + atUnlock + " / " + later);
    }
    assertTrue(appearances(tiers, 5, 50) > 6, "Max reputation did not favor top tier");
  }

  private static double appearances(int[] tiers, int wanted, int cred) {
    int total = 0;
    for (int day = 0; day < 2000; day++) {
      var chosen = DailyStock.select(tiers, 8137, day, cred, 0);
      total += (int) chosen.stream().filter(i -> tiers[i] == wanted).count();
    }
    return total / 2000.;
  }

  @Test
  void unevenTierSizesDoNotMakeNewTierDominateAtUnlock() {
    int[] tiers = new int[107];
    Arrays.fill(tiers, 0, 7, 1);
    Arrays.fill(tiers, 7, 107, 2);
    double average = appearances(tiers, 2, 10);
    assertTrue(average > 1 && average < 5.5, "Tier count biased tier weighting: " + average);
  }

  @Test
  void sparseCataloguesAndLargeDayValuesRemainSafe() {
    assertTrue(DailyStock.select(new int[0], 1, Long.MAX_VALUE, 0, -1).isEmpty());
    assertEquals(1, DailyStock.select(new int[] {1}, 1, 0, 0, 0).cardinality());
    int[] tiers = catalogue();
    var chosen = DailyStock.select(tiers, Long.MIN_VALUE, Long.MAX_VALUE, 50, 0);
    assertEquals(10, chosen.cardinality());
  }
}
