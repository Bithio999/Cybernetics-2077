package dev.cybernetics2077.mob;

/** Contract difficulty is the installed capacity above the human limit, not implant tier. */
public final class CyberpsychoRules {
  public static final int BASE_CAPACITY = 150;
  public static final int BAND = 25;

  public static int rank(int installedCapacity) {
    if (installedCapacity <= BASE_CAPACITY)
      throw new IllegalArgumentException("A cyberpsycho must exceed 150 capacity");
    return (int) Math.min(6, ((long) installedCapacity - BASE_CAPACITY - 1) / BAND + 1);
  }

  private CyberpsychoRules() {}
}
