package dev.cybernetics2077.cyberware;

import net.minecraft.world.item.Item;

/** Tiered, single-socket implant contract. */
public class CyberwareItem extends Item {
  private final CyberwareCategory category;
  private final int capacity;
  private final CyberwareTier tier;

  public CyberwareItem(Properties properties, CyberwareCategory category, int capacity) {
    this(properties, category, capacity, CyberwareTier.ONE);
  }

  public CyberwareItem(
      Properties properties, CyberwareCategory category, int capacity, CyberwareTier tier) {
    super(properties.stacksTo(1));
    if (tier == null || category == null || capacity < 0 || capacity > 100000)
      throw new IllegalArgumentException("Invalid cyberware definition");
    this.tier = tier;
    this.category = category;
    this.capacity = capacity;
  }

  public CyberwareCategory category() {
    return category;
  }

  public CyberwareTier tier() {
    return tier;
  }

  @Override
  public void appendHoverText(
      net.minecraft.world.item.ItemStack stack,
      TooltipContext context,
      java.util.List<net.minecraft.network.chat.Component> lines,
      net.minecraft.world.item.TooltipFlag flag) {
    lines.add(
        net.minecraft.network.chat.Component.translatable(
                "cybernetics2077.implant.tier", tier.level)
            .withColor(tier.color & 0xffffff));
    lines.add(
        net.minecraft.network.chat.Component.translatable(category.translation())
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    lines.add(
        net.minecraft.network.chat.Component.translatable(
                "cybernetics2077.implant.capacity", capacity)
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    if (this == dev.cybernetics2077.Cybernetics2077.OPTICS.get())
      lines.add(
          net.minecraft.network.chat.Component.translatable("cybernetics2077.optics.description")
              .withStyle(net.minecraft.ChatFormatting.AQUA));
    if (this == dev.cybernetics2077.Cybernetics2077.CYBERDECK.get())
      lines.add(
          net.minecraft.network.chat.Component.translatable("cybernetics2077.deck.description")
              .withStyle(net.minecraft.ChatFormatting.AQUA));
    if (this == dev.cybernetics2077.Cybernetics2077.MANTIS.get())
      lines.add(
          net.minecraft.network.chat.Component.translatable("cybernetics2077.mantis.description")
              .withStyle(net.minecraft.ChatFormatting.AQUA));
  }

  public int capacityCost() {
    return capacity;
  }
}
