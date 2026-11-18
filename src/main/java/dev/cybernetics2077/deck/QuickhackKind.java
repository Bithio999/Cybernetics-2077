package dev.cybernetics2077.deck;

/** Named programs, with Cyberpunk starting tiers and Minecraft-scaled effects. */
public enum QuickhackKind {
  SHORT_CIRCUIT("short_circuit", 1, "COMBAT", "Electrical"),
  OVERHEAT("overheat", 1, "COMBAT", "Thermal"),
  CONTAGION("contagion", 2, "COMBAT", "Chemical"),
  REBOOT_OPTICS("reboot_optics", 1, "CONTROL", "None"),
  CRIPPLE_MOVEMENT("cripple_movement", 2, "CONTROL", "None"),
  SYNAPSE_BURNOUT("synapse_burnout", 3, "COMBAT", "Physical");
  public final String id, category, damageType;
  public final int firstTier;

  QuickhackKind(String id, int firstTier, String category, String damageType) {
    this.id = id;
    this.firstTier = firstTier;
    this.category = category;
    this.damageType = damageType;
  }

  public boolean control() {
    return this == REBOOT_OPTICS || this == CRIPPLE_MOVEMENT;
  }

  public int cost(int tier) {
    return switch (this) {
      case SHORT_CIRCUIT -> tier + 1;
      case OVERHEAT, CONTAGION -> tier + 2;
      case REBOOT_OPTICS -> (tier + 3) / 2;
      case CRIPPLE_MOVEMENT -> tier;
      case SYNAPSE_BURNOUT -> tier + 3;
    };
  }

  public float damage(int tier) {
    return switch (this) {
      case SHORT_CIRCUIT ->
          switch (tier) {
            case 1 -> 8;
            case 2 -> 11;
            case 3 -> 14;
            case 4 -> 18;
            default -> 22;
          };
      case OVERHEAT -> 8 + 4 * tier;
      case CONTAGION -> 6 * tier;
      case SYNAPSE_BURNOUT -> 6 + 6 * tier;
      default -> 0;
    };
  }

  public int duration(int tier) {
    return switch (this) {
      case OVERHEAT -> 80;
      case CONTAGION -> 160;
      case REBOOT_OPTICS -> 60 + 20 * tier;
      case CRIPPLE_MOVEMENT -> 40 + 20 * tier;
      default -> 0;
    };
  }

  public String icon() {
    return this == SHORT_CIRCUIT ? "short_circuit_tier_1" : id;
  }
}
