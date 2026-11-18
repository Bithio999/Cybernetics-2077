package dev.cybernetics2077.deck;

import dev.cybernetics2077.cyberware.CyberwareTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class QuickhackItem extends Item {
  private final CyberwareTier tier;

  public QuickhackItem() {
    this(CyberwareTier.ONE);
  }

  public QuickhackItem(CyberwareTier tier) {
    super(new Properties().stacksTo(1));
    this.tier = tier;
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
        Component.translatable(
                "cybernetics2077.short_circuit.stats",
                tier.shortCircuitCost(),
                tier.shortCircuitDamage())
            .withStyle(ChatFormatting.GRAY));
  }
}
