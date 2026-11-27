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

/** Readable dossier; ownership and payment are checked against the server ledger. */
public final class BountyContract extends Item {
  public BountyContract() {
    super(new Properties().stacksTo(1));
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    var t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    if (!t.hasUUID("token")) {
      lines.add(
          Component.literal("Accept a bounty from an Afterlife fixer.")
              .withStyle(ChatFormatting.GRAY));
      return;
    }
    lines.add(Component.literal(t.getString("title")).withStyle(ChatFormatting.AQUA));
    lines.add(Component.literal(BountyContracts.objective(t)).withStyle(ChatFormatting.YELLOW));
    lines.add(
        Component.literal(
                BountyRules.rankName(Math.clamp(t.getInt("rank"), 1, 6))
                    + " / "
                    + t.getInt("implants")
                    + " implants / "
                    + t.getInt("guards")
                    + " guards")
            .withStyle(ChatFormatting.GRAY));
    lines.add(
        Component.literal("Lead: " + t.getInt("x") + ", " + t.getInt("y") + ", " + t.getInt("z"))
            .withStyle(ChatFormatting.YELLOW));
    lines.add(
        Component.literal(
                "€$" + t.getInt("eddies") + " + bonuses / +" + t.getInt("cred") + " Street Cred")
            .withStyle(ChatFormatting.GOLD));
    lines.add(
        Component.literal("Optional: " + BountyRules.specialty(t.getInt("profile")))
            .withStyle(ChatFormatting.GRAY));
    lines.add(
        Component.literal("Optional: no civilian kills; survive the hunt.")
            .withStyle(ChatFormatting.GRAY));
    lines.add(
        Component.literal(
                "Hold scan for directions. Return to "
                    + BountyRules.profileName(t.getInt("profile"))
                    + ".")
            .withStyle(ChatFormatting.AQUA));
    lines.add(
        Component.literal(
                "Status is tracked by your fixer; a copied shard cannot duplicate payment.")
            .withStyle(ChatFormatting.DARK_GRAY));
  }

  @Override
  public InteractionResult interactLivingEntity(
      ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
    if (!(entity instanceof CityResident fixer) || !fixer.fixer()) return InteractionResult.PASS;
    if (player instanceof ServerPlayer p) {
      var t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
      var job = BountyContracts.job(p);
      if (t.hasUUID("owner")
          && t.hasUUID("token")
          && t.getUUID("owner").equals(p.getUUID())
          && job.hasUUID("token")
          && job.getUUID("token").equals(t.getUUID("token"))
          && job.getInt("stage") == 2) BountyContracts.claim(p, fixer);
      else return InteractionResult.PASS;
    }
    return InteractionResult.sidedSuccess(player.level().isClientSide);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (player instanceof ServerPlayer p) {
      var t =
          player
              .getItemInHand(hand)
              .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
              .copyTag();
      var job = BountyContracts.job(p);
      boolean valid =
          t.hasUUID("owner")
              && t.getUUID("owner").equals(p.getUUID())
              && t.hasUUID("token")
              && job.hasUUID("token")
              && t.getUUID("token").equals(job.getUUID("token"));
      if (valid && job.getInt("stage") == 4) {
        if (!BountyContracts.recover(p))
          p.displayClientMessage(
              Component.literal(
                  "Approach the sealed cache and use this shard within 4 blocks, with clear"
                      + " sight."),
              true);
        return InteractionResultHolder.success(player.getItemInHand(hand));
      }
    }
    if (!level.isClientSide)
      player.displayClientMessage(
          Component.literal(
              "Hold scan for your lead. Return to the issuing fixer network for payment."),
          true);
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
  }
}
