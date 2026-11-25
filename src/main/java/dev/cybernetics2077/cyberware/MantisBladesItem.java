package dev.cybernetics2077.cyberware;

/** The physical Mantis Blades family; tier upgrades retain the same articulated mechanism. */
public final class MantisBladesItem extends CyberwareItem {
  private final MantisVariant variant;

  public MantisVariant variant() {
    return variant;
  }

  public MantisBladesItem(CyberwareTier tier) {
    this(tier, MantisVariant.PHYSICAL);
  }

  public MantisBladesItem(CyberwareTier tier, MantisVariant variant) {
    super(new Properties(), CyberwareCategory.ARMS, tier.bladeCapacity(), tier);
    this.variant = java.util.Objects.requireNonNull(variant);
    if (tier.level < 2) throw new IllegalArgumentException("Mantis Blades start at tier 2");
  }
}
