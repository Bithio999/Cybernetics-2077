package dev.cybernetics2077.cyberware;

public enum CyberwareCategory {
  FRONTAL_CORTEX("frontal_cortex", 3),
  OPERATING_SYSTEM("operating_system", 1),
  ARMS("arms", 1),
  FACE("face", 1),
  SKELETON("skeleton", 3),
  HANDS("hands", 2),
  NERVOUS_SYSTEM("nervous_system", 3),
  CIRCULATORY_SYSTEM("circulatory_system", 3),
  INTEGUMENTARY_SYSTEM("integumentary_system", 3),
  LEGS("legs", 1);

  public final String id;
  public final int sockets;

  CyberwareCategory(String id, int sockets) {
    this.id = id;
    this.sockets = sockets;
  }

  public String translation() {
    return "cybernetics2077.category." + id;
  }

  public int firstSlot() {
    int n = 0;
    for (var c : values()) {
      if (c == this) return n;
      n += c.sockets;
    }
    throw new AssertionError();
  }

  public static int totalSlots() {
    int n = 0;
    for (var c : values()) n += c.sockets;
    return n;
  }

  public static CyberwareCategory forSlot(int slot) {
    if (slot < 0 || slot >= totalSlots()) throw new IndexOutOfBoundsException(slot);
    for (var c : values()) if (slot < c.firstSlot() + c.sockets) return c;
    throw new AssertionError();
  }
}
