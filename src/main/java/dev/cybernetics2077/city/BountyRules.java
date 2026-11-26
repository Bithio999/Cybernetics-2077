package dev.cybernetics2077.city;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Immutable daily dossiers. This layer does not load chunks, spawn entities or award money. */
public final class BountyRules {
  public record Offer(
      UUID id, int slot, int rank, int profile, long day, String alias, String mob) {}

  private static final int[] PAY = {1800, 3500, 6000, 10000, 16000, 24000};
  private static final String[] MARA = {
    "Relay", "Dead Signal", "Wiretap", "Red Ledger", "Black Frequency", "Ghost Circuit"
  };
  private static final String[] REN = {
    "Copperhead", "Iron Jack", "Crowbar", "Red Knuckle", "Lockjaw", "Grinder"
  };

  public static int maxRank(int cred) {
    return Math.clamp(cred, 0, 50) / 10 + 1;
  }

  public static int tier(int rank) {
    check(rank);
    return Math.min(5, rank);
  }

  public static int implants(int rank) {
    check(rank);
    return rank;
  }

  public static int guards(int rank) {
    check(rank);
    return rank == 6 ? 3 : (rank - 1) / 2;
  }

  public static int pay(int rank) {
    check(rank);
    return PAY[rank - 1];
  }

  public static int cred(int rank) {
    check(rank);
    return rank + 1;
  }

  public static int requiredCred(int rank) {
    check(rank);
    return (rank - 1) * 10;
  }

  public static String rankName(int rank) {
    check(rank);
    return rank == 6 ? "ELITE T5" : "TIER " + rank;
  }

  public static String profileName(int profile) {
    return profile == 0 ? "MARA VOSS" : "REN CALDER";
  }

  public static String specialty(int profile) {
    return profile == 0
        ? "Identify the target before engaging"
        : "Finish within 90 seconds of engaging";
  }

  public static List<Offer> offers(UUID fixer, UUID player, long day, int cred, int profile) {
    var random =
        new SplittableRandom(
            fixer.getMostSignificantBits()
                ^ player.getLeastSignificantBits()
                ^ day * 0x9e3779b97f4a7c15L);
    int max = maxRank(cred);
    int[] ranks = {max, Math.max(1, max - 1), 1 + random.nextInt(max)};
    var result = new ArrayList<Offer>();
    for (int slot = 0; slot < 3; slot++) {
      int rank = ranks[slot];
      String key = fixer + ":" + player + ":" + day + ":" + slot + ":" + rank + ":" + profile;
      String mob =
          profile == 0
              ? (random.nextBoolean() ? "minecraft:skeleton" : "minecraft:pillager")
              : (rank >= 3 && random.nextBoolean() ? "minecraft:vindicator" : "minecraft:husk");
      String[] names = profile == 0 ? MARA : REN;
      result.add(
          new Offer(
              UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)),
              slot,
              rank,
              profile,
              day,
              names[random.nextInt(names.length)] + "-" + (10 + random.nextInt(90)),
              mob));
    }
    return List.copyOf(result);
  }

  public static int optionalCount(boolean specialty, boolean civilians, boolean survival) {
    return (specialty ? 1 : 0) + (civilians ? 1 : 0) + (survival ? 1 : 0);
  }

  public static String rating(int count) {
    return count >= 3 ? "S" : count == 2 ? "A" : count == 1 ? "B" : "C";
  }

  public static int bonusPercent(boolean specialty, boolean civilians, boolean survival) {
    return (specialty ? 20 : 0) + (civilians ? 10 : 0) + (survival ? 10 : 0);
  }

  private static void check(int rank) {
    if (rank < 1 || rank > 6) throw new IllegalArgumentException("Invalid bounty rank");
  }

  private BountyRules() {}
}
