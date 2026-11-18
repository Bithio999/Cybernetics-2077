package dev.cybernetics2077.cyberware;

/** Basic Kiroshi Optics; higher tiers extend Minecraft's target scanning range. */
public final class OpticsItem extends CyberwareItem {
  public OpticsItem(CyberwareTier tier) {
    super(new Properties(), CyberwareCategory.FACE, tier.opticsCapacity(), tier);
  }
}
