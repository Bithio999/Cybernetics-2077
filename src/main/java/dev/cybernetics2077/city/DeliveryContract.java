package dev.cybernetics2077.city;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class DeliveryContract extends Item {
  public DeliveryContract() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    var t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    lines.add(Component.translatable(DeliveryContracts.NAME).withStyle(ChatFormatting.AQUA));
    lines.add(
        Component.translatable("cybernetics2077.contract.objective")
            .withStyle(ChatFormatting.GRAY));
    if (t.contains("dimension")) {
      lines.add(
          Component.translatable(
                  "cybernetics2077.contract.coordinates",
                  t.getInt("x"),
                  t.getInt("y"),
                  t.getInt("z"))
              .withStyle(ChatFormatting.YELLOW));
      lines.add(Component.literal(t.getString("dimension")).withStyle(ChatFormatting.DARK_GRAY));
    }
    lines.add(
        Component.translatable(
                "cybernetics2077.contract.reward", DeliveryContracts.EDDIES, DeliveryContracts.CRED)
            .withStyle(ChatFormatting.GOLD));
    lines.add(
        Component.translatable("cybernetics2077.contract.use").withStyle(ChatFormatting.GRAY));
    lines.add(
        Component.translatable("cybernetics2077.contract.virtual")
            .withStyle(ChatFormatting.DARK_GRAY));
  }

  @Override
  public InteractionResult interactLivingEntity(
      ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
    if (entity instanceof CityResident n && !n.fixer()) {
      if (player instanceof ServerPlayer p) DeliveryContracts.turnIn(p, n, stack);
      return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
    return InteractionResult.PASS;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
    if (!level.isClientSide)
      p.displayClientMessage(Component.translatable("cybernetics2077.contract.read"), true);
    return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide);
  }
}
