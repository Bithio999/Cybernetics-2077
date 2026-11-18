package dev.cybernetics2077.cyberware;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class AugmentItem extends CyberwareItem {
  private final Augment augment;

  public AugmentItem(Augment augment, CyberwareTier tier) {
    super(new Properties(), augment.category, augment.capacity, tier);
    if (tier.level < augment.firstTier)
      throw new IllegalArgumentException("Unavailable starting tier");
    this.augment = augment;
  }

  public Augment augment() {
    return augment;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    super.appendHoverText(stack, context, lines, flag);
    for (var line : augment.details(tier().level))
      lines.add(Component.literal(line).withStyle(ChatFormatting.AQUA));
  }
}
