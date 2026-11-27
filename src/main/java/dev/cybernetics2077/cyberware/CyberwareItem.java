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

  public boolean iconic() {
    return false;
  }

  @Override
  public net.minecraft.network.chat.Component getName(net.minecraft.world.item.ItemStack stack) {
    return super.getName(stack).copy().withColor(tier.color & 0xffffff);
  }

  @Override
  public void appendHoverText(
      net.minecraft.world.item.ItemStack stack,
      TooltipContext context,
      java.util.List<net.minecraft.network.chat.Component> lines,
      net.minecraft.world.item.TooltipFlag flag) {
    if (iconic())
      lines.add(
          net.minecraft.network.chat.Component.translatable("cybernetics2077.implant.iconic")
              .withStyle(net.minecraft.ChatFormatting.GOLD));
    lines.add(
        net.minecraft.network.chat.Component.translatable(
                "cybernetics2077.implant.meta", tier.level, capacity)
            .withColor(tier.color & 0xffffff));
    lines.add(
        net.minecraft.network.chat.Component.translatable(category.translation())
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    if (this instanceof OpticsItem)
      lines.add(
          net.minecraft.network.chat.Component.translatable(
                  "cybernetics2077.optics.description", tier.scanRange())
              .withStyle(net.minecraft.ChatFormatting.AQUA));
    if (this instanceof dev.cybernetics2077.deck.CyberdeckItem deck) {
      lines.add(
          net.minecraft.network.chat.Component.translatable(
                  "cybernetics2077.deck.description", deck.ram(), tier.programSlots())
              .withStyle(net.minecraft.ChatFormatting.AQUA));
      lines.add(
          net.minecraft.network.chat.Component.translatable(
                  "cybernetics2077.deck.stats",
                  String.format(java.util.Locale.ROOT, "%.1f", deck.ramPerSecond()),
                  tier.queueLimit(),
                  Math.round(deck.damageBonus() * 100))
              .withStyle(net.minecraft.ChatFormatting.GRAY));
      if (!deck.specialty().isEmpty())
        lines.add(
            net.minecraft.network.chat.Component.literal(deck.specialty())
                .withStyle(net.minecraft.ChatFormatting.AQUA));
      if (deck.relayTargets() > 0)
        lines.add(
            net.minecraft.network.chat.Component.translatable(
                    "cybernetics2077.deck.raven.relay",
                    deck.relayTargets(),
                    deck.relayRange(),
                    Math.round(deck.relayDamage() * 100))
                .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
    if (this instanceof MantisBladesItem blades) {
      lines.add(
          net.minecraft.network.chat.Component.literal(blades.variant().effect())
              .withColor(blades.variant().color));
      lines.add(
          net.minecraft.network.chat.Component.literal("Lunges guarantee the variant effect.")
              .withStyle(net.minecraft.ChatFormatting.GRAY));
      lines.add(
          net.minecraft.network.chat.Component.translatable("cybernetics2077.mantis.description")
              .withStyle(net.minecraft.ChatFormatting.AQUA));
      lines.add(
          net.minecraft.network.chat.Component.translatable(
                  "cybernetics2077.mantis.stats",
                  8 * tier.bladeMultiplier(),
                  10 * tier.bladeMultiplier(),
                  14 * tier.bladeMultiplier())
              .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
  }

  public int capacityCost() {
    return capacity;
  }
}
