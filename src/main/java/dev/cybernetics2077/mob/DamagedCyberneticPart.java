package dev.cybernetics2077.mob;

import dev.cybernetics2077.city.CyberwareCatalogue;
import dev.cybernetics2077.cyberware.*;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class DamagedCyberneticPart extends Item {
  public static final int DISCOUNT_PERCENT = 30;
  private final CyberwareTier tier;

  public DamagedCyberneticPart(CyberwareTier tier) {
    super(new Properties());
    this.tier = tier;
  }

  public CyberwareTier tier() {
    return tier;
  }

  public static int price(CyberwareCatalogue.Offer offer) {
    return Math.max(1, (offer.price() * (100 - DISCOUNT_PERCENT) + 99) / 100);
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
        Component.translatable("cybernetics2077.salvage.discount", DISCOUNT_PERCENT, tier.level)
            .withStyle(ChatFormatting.AQUA));
    lines.add(
        Component.translatable("cybernetics2077.salvage.requirements")
            .withStyle(ChatFormatting.GRAY));
    lines.add(
        Component.translatable("cybernetics2077.salvage.consumed").withStyle(ChatFormatting.GRAY));
  }
}
