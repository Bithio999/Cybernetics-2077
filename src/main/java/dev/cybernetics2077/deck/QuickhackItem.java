package dev.cybernetics2077.deck;

import dev.cybernetics2077.cyberware.CyberwareTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class QuickhackItem extends Item {
  private final CyberwareTier tier;
  private final QuickhackKind kind;

  public QuickhackItem() {
    this(CyberwareTier.ONE);
  }

  public QuickhackItem(CyberwareTier tier) {
    this(tier, QuickhackKind.SHORT_CIRCUIT);
  }

  public QuickhackItem(CyberwareTier tier, QuickhackKind kind) {
    super(new Properties().stacksTo(1));
    if (kind == null || tier == null || tier.level < kind.firstTier)
      throw new IllegalArgumentException("Unavailable quickhack tier");
    this.tier = tier;
    this.kind = kind;
  }

  public QuickhackKind kind() {
    return kind;
  }

  public int cost() {
    return kind.cost(tier.level);
  }

  public float damage() {
    return kind.damage(tier.level);
  }

  public int duration() {
    return kind.duration(tier.level);
  }

  public CyberwareTier tier() {
    return tier;
  }

  @Override
  public Component getName(ItemStack stack) {
    return super.getName(stack).copy().withColor(tier.color & 0xffffff);
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    lines.add(
        Component.translatable("cybernetics2077.implant.tier", tier.level)
            .withColor(tier.color & 0xffffff));
    lines.add(
        Component.translatable("cybernetics2077.quickhack.description", tier.level)
            .withStyle(ChatFormatting.AQUA));
    lines.add(
        Component.translatable("cybernetics2077.hack." + kind.id + ".description")
            .withStyle(ChatFormatting.AQUA));
    if (kind.control())
      lines.add(
          Component.translatable("cybernetics2077.hack.control_cost", cost())
              .withStyle(ChatFormatting.GRAY));
    else
      lines.add(
          Component.translatable(
                  "cybernetics2077.hack.stats",
                  cost(),
                  String.format(java.util.Locale.ROOT, "%.0f", damage()),
                  kind.damageType)
              .withStyle(ChatFormatting.GRAY));
    if (duration() > 0)
      lines.add(
          Component.translatable("cybernetics2077.hack.duration", duration() / 20)
              .withStyle(ChatFormatting.GRAY));
  }
}
