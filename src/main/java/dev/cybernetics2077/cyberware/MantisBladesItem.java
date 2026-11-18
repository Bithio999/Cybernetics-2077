package dev.cybernetics2077.cyberware;

/** The physical Mantis Blades family; tier upgrades retain the same articulated mechanism. */
public final class MantisBladesItem extends CyberwareItem {
  public MantisBladesItem(CyberwareTier tier) {
    super(new Properties(), CyberwareCategory.ARMS, tier.bladeCapacity(), tier);
  }
}
