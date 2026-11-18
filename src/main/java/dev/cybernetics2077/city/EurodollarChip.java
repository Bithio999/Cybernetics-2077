package dev.cybernetics2077.city;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class EurodollarChip extends Item {
  public static final int VALUE = 100;

  public EurodollarChip() {
    super(new Properties());
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (!level.isClientSide) {
      var account = player.getData(CityContent.ACCOUNT);
      if (account.eddies() > 1_000_000_000L - VALUE) return InteractionResultHolder.fail(stack);
      account.credit(VALUE);
      stack.shrink(1);
      player.displayClientMessage(
          Component.translatable("cybernetics2077.city.deposit", account.eddies()), true);
    }
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  @Override
  public void appendHoverText(
      ItemStack s, TooltipContext c, java.util.List<Component> lines, TooltipFlag f) {
    lines.add(
        Component.translatable("cybernetics2077.city.chip.description", VALUE)
            .withStyle(net.minecraft.ChatFormatting.GRAY));
  }
}
