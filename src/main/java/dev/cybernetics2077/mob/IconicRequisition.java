package dev.cybernetics2077.mob;

import dev.cybernetics2077.cyberware.CyberwareTier;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** A consumable purchase authorization, never an implant or salvage discount. */
public final class IconicRequisition extends Item {
  private final CyberwareTier tier;

  public IconicRequisition(CyberwareTier tier) {
    super(new Properties());
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
        Component.translatable("cybernetics2077.iconic.requisition", tier.level)
            .withStyle(ChatFormatting.GOLD));
    lines.add(
        Component.translatable("cybernetics2077.iconic.requirements")
            .withStyle(ChatFormatting.GRAY));
    lines.add(
        Component.translatable("cybernetics2077.iconic.consumed").withStyle(ChatFormatting.GRAY));
  }
}
