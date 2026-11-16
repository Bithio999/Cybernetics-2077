package dev.cybernetics2077.cyberware;

import net.minecraft.world.item.Item;

/** Base contract for later implants. This first release registers no implant items. */
public class CyberwareItem extends Item {
  private final CyberwareCategory category;
  private final int capacity;

  public CyberwareItem(Properties properties, CyberwareCategory category, int capacity) {
    super(properties.stacksTo(1));
    if (category == null || capacity < 0 || capacity > 100000)
      throw new IllegalArgumentException("Invalid cyberware definition");
    this.category = category;
    this.capacity = capacity;
  }

  public CyberwareCategory category() {
    return category;
  }

  public int capacityCost() {
    return capacity;
  }
}
