package dev.cybernetics2077.city;

import java.util.*;

/** Deterministic daily assortment. No save data, world generation or per-tick random rolls. */
public final class DailyStock {
  public static final int LIMIT = 10;

  /** Tier weights apply to buckets, so a tier with more catalogue entries gets no extra bias. */
  public static double tierWeight(int tier, int cred) {
    if (tier < 1 || tier > 5 || (tier - 1) * 10 > cred) return 0;
    double distance = tier - (.15 + Math.clamp(cred, 0, 50) / 10.0);
    return Math.exp(-1.1 * distance * distance);
  }

  public static BitSet select(int[] tiers, long clinicSeed, long day, int cred, int starterOptics) {
    cred = Math.clamp(cred, 0, 50);
    var result = new BitSet(tiers.length);
    var pool = new ArrayList<Integer>();
    boolean pin =
        cred <= 10
            && starterOptics >= 0
            && starterOptics < tiers.length
            && tiers[starterOptics] == 1;
    for (int i = 0; i < tiers.length; i++) {
      if (tierWeight(tiers[i], cred) == 0) continue;
      if (pin && i == starterOptics) result.set(i);
      else pool.add(i);
    }
    int eligible = pool.size() + result.cardinality();
    if (pool.size() <= 1) {
      pool.forEach(result::set);
      return result;
    }
    // Today's featured item was yesterday's omitted item. Even small starter assortments
    // visibly change every day; the permanently stocked starter eye is never omitted.
    int featured =
        (int)
            Math.floorMod(
                Math.floorMod(day, pool.size()) + Math.floorMod(clinicSeed, pool.size()),
                pool.size());
    int include = pool.get(featured);
    int omit = pool.get(Math.floorMod(featured + 1, pool.size()));
    result.set(include);
    pool.remove(Integer.valueOf(include));
    pool.remove(Integer.valueOf(omit));
    int count = Math.min(LIMIT, eligible - 1);
    var random = new SplittableRandom(mix(clinicSeed ^ mix(day) ^ mix(cred)));
    while (result.cardinality() < count && !pool.isEmpty()) {
      int[] counts = new int[6];
      for (int i : pool) counts[tiers[i]]++;
      double total = 0;
      for (int tier = 1; tier <= 5; tier++) if (counts[tier] > 0) total += tierWeight(tier, cred);
      double roll = random.nextDouble(total);
      int selected = pool.getLast();
      for (int i : pool) {
        roll -= tierWeight(tiers[i], cred) / counts[tiers[i]];
        if (roll < 0) {
          selected = i;
          break;
        }
      }
      result.set(selected);
      pool.remove(Integer.valueOf(selected));
    }
    return result;
  }

  public static long mix(long value) {
    value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
    value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
    return value ^ (value >>> 31);
  }

  private DailyStock() {}
}
