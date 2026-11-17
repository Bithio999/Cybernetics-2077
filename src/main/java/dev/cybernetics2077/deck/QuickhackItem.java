package dev.cybernetics2077.deck;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class QuickhackItem extends Item {
  public QuickhackItem() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    lines.add(
        Component.translatable("cybernetics2077.implant.tier", 1).withStyle(ChatFormatting.WHITE));
    lines.add(
        Component.translatable("cybernetics2077.quickhack.description")
            .withStyle(ChatFormatting.AQUA));
    lines.add(
        Component.translatable("cybernetics2077.short_circuit.stats")
            .withStyle(ChatFormatting.GRAY));
  }
}
