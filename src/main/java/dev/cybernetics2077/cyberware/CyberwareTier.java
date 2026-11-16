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
}
