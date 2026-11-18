package dev.cybernetics2077.cyberware;

/** Effect strength, independent of capacity usage. All implants declare exactly one tier. */
public enum CyberwareTier {
  ONE(1, 0xffc7d5da),
  TWO(2, 0xff6fe69c),
  THREE(3, 0xff68baf5),
  FOUR(4, 0xffbf8dff),
  FIVE(5, 0xffffbd58);
  public final int level, color;

  CyberwareTier(int level, int color) {
    this.level = level;
    this.color = color;
  }

  public static CyberwareTier of(int level) {
    if (level < 1 || level > 5) throw new IllegalArgumentException("Cyberware tier must be 1–5");
    return values()[level - 1];
  }

  public int scanRange() {
    return 24 + level * 8;
  }

  public int opticsCapacity() {
    return 3 + level;
  }

  public int deckCapacity() {
    return 4 + level * 4;
  }

  public int bladeCapacity() {
    return 9 + level * 3;
  }

  public int ram() {
    return 2 + level * 2;
  }

  public int programSlots() {
    return switch (this) {
      case ONE -> 2;
      case TWO -> 3;
      case THREE -> 4;
      case FOUR -> 6;
      case FIVE -> 8;
    };
  }

  public int queueLimit() {
    return switch (this) {
      case ONE, TWO -> 2;
      case THREE, FOUR -> 3;
      case FIVE -> 4;
    };
  }

  public float ramPerSecond() {
    return .1f + level * .1f;
  }

  public float quickhackBonus() {
    return (level - 1) * .05f;
  }

  public int uploadTicks() {
    return 22 - level * 2;
  }

  public float bladeMultiplier() {
    return 1 + (level - 1) * .25f;
  }

  public int shortCircuitDamage() {
    return switch (this) {
      case ONE -> 8;
      case TWO -> 11;
      case THREE -> 14;
      case FOUR -> 18;
      case FIVE -> 22;
    };
  }

  public int shortCircuitCost() {
    return level + 1;
  }
}
