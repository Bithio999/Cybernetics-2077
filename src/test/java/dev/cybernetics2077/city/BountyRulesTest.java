package dev.cybernetics2077.city;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class BountyRulesTest {
  private final UUID fixer = UUID.fromString("f03eba4d-b8ce-4bd5-a6b8-d4220f3a9520"),
      player = UUID.fromString("8db57cca-bccb-4998-a676-23c8eb9e3ce2");

  @Test
  void everyStreetCredGateOffersOnlyUnlockedRanksAndIncludesStrongest() {
    for (int cred = 0; cred <= 50; cred++) {
      var offers = BountyRules.offers(fixer, player, 8, cred, 0);
      assertEquals(3, offers.size());
      assertEquals(cred / 10 + 1, offers.getFirst().rank());
      for (var offer : offers) {
        assertTrue(BountyRules.requiredCred(offer.rank()) <= cred);
        assertTrue(BountyRules.tier(offer.rank()) <= 5);
      }
    }
    assertEquals(5, BountyRules.tier(6));
    assertEquals(6, BountyRules.implants(6));
    assertEquals(3, BountyRules.guards(6));
  }

  @Test
  void dailyBoardIsStableWithinCredBracketAndDistinctAcrossPlayersFixersDaysAndProfiles() {
    var board = BountyRules.offers(fixer, player, 8, 20, 0);
    assertEquals(board, BountyRules.offers(fixer, player, 8, 29, 0));
    assertEquals(3, board.stream().map(BountyRules.Offer::id).distinct().count());
    assertNotEquals(board, BountyRules.offers(fixer, player, 9, 20, 0));
    assertNotEquals(board, BountyRules.offers(fixer, UUID.randomUUID(), 8, 20, 0));
    assertNotEquals(board, BountyRules.offers(UUID.randomUUID(), player, 8, 20, 0));
    assertNotEquals(board, BountyRules.offers(fixer, player, 8, 20, 1));
    assertTrue(
        board.stream()
            .allMatch(
                o ->
                    o.kind() == BountyRules.Kind.CYBERPSYCHO
                        || o.mob().equals("minecraft:skeleton")
                        || o.mob().equals("minecraft:pillager")));
  }

  @Test
  void optionalBonusesNeverReduceBasePayAndRatingsCoverAllOutcomes() {
    for (int mask = 0; mask < 8; mask++) {
      boolean s = (mask & 1) != 0, c = (mask & 2) != 0, v = (mask & 4) != 0;
      assertEquals(Integer.bitCount(mask), BountyRules.optionalCount(s, c, v));
      assertEquals((s ? 20 : 0) + (c ? 30 : 0) + (v ? 10 : 0), BountyRules.bonusPercent(s, c, v));
    }
    assertEquals("S", BountyRules.rating(3));
    assertEquals("A", BountyRules.rating(2));
    assertEquals("B", BountyRules.rating(1));
    assertEquals("C", BountyRules.rating(0));
    for (int rank = 2; rank <= 6; rank++)
      assertTrue(BountyRules.pay(rank) > BountyRules.pay(rank - 1));
  }

  @Test
  void boardsIncludeEveryJobTypeAndReducedProgressionRewards() {
    var board = BountyRules.offers(fixer, player, 8, 50, 0);
    assertEquals(
        Set.of(BountyRules.Kind.values()),
        new HashSet<>(board.stream().map(BountyRules.Offer::kind).toList()));
    int[] prices = {600, 1000, 1600, 2400, 3400, 4800};
    for (int rank = 1; rank <= 6; rank++) {
      assertEquals(prices[rank - 1], BountyRules.pay(rank));
      assertEquals((rank + 1) / 2, BountyRules.cred(rank));
      assertTrue(BountyRules.pay(rank, BountyRules.Kind.CYBERPSYCHO) > BountyRules.pay(rank));
      assertTrue(
          BountyRules.implants(rank, BountyRules.Kind.CYBERPSYCHO) >= BountyRules.implants(rank));
    }
    assertEquals(BountyRules.Kind.HUNT, BountyRules.Kind.from(""));
  }

  @Test
  void malformedRanksAreRejected() {
    for (int rank : new int[] {-1, 0, 7, 999})
      assertThrows(IllegalArgumentException.class, () -> BountyRules.tier(rank));
  }
}
