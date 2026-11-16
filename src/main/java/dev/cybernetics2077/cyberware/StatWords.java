package dev.cybernetics2077.cyberware;

/** Menu data packets carry signed 16-bit values; preserve full positive equipment totals. */
public final class StatWords {
  public static int word(int value, int part) {
    return value >>> (part * 16) & 0xffff;
  }

  public static int combine(int low, int high) {
    return low & 0xffff | (high & 0xffff) << 16;
  }

  private StatWords() {}
}
