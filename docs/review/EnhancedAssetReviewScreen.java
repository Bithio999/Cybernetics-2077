package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class EnhancedAssetReviewScreen extends Screen {
  EnhancedAssetReviewScreen() {
    super(Component.literal("Enhanced mobs"));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0xff0b131c);
    g.drawString(font, "CHROMED HOSTILES // RARE CYBERNETIC VARIANTS", 22, 20, 0xff79dae5);
    g.drawString(
        font,
        "Vanilla bodies. Installed implants. Salvage for matching-tier discounts.",
        22,
        37,
        0xffa2b2bf);
    for (int i = 0; i < 3; i++) {
      int cx = width / 6 + width * i / 3;
      g.fill(cx - 82, 64, cx + 82, 260, 0xff15222e);
      var mob = (LivingEntity) minecraft.level.getEntity(EnhancedClientReview.mobs[i]);
      if (mob == null || mob.getCustomName() == null)
        throw new IllegalStateException("Missing synchronized chromed mob");
      InventoryScreen.renderEntityInInventoryFollowsMouse(
          g, cx - 75, 65, cx + 75, 238, 65, .0625f, cx - 15, 150, mob);
      g.drawCenteredString(font, mob.getType().getDescription(), cx, 243, 0xffddebf0);
    }
    int start = width / 2 - 130;
    for (int t = 1; t <= 5; t++) {
      var stack = new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(t - 1).get());
      if (minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0)
          == minecraft.getModelManager().getMissingModel())
        throw new IllegalStateException("Missing salvage item model");
      int x = start + (t - 1) * 58;
      g.pose().pushPose();
      g.pose().translate(x, 280, 0);
      g.pose().scale(1.5f, 1.5f, 1.5f);
      g.renderItem(stack, 0, 0);
      g.pose().popPose();
      g.drawString(font, "TIER " + t, x - 5, 311, 0xffa9cbd2);
    }
    g.drawCenteredString(
        font,
        "1 matching part + 70% of the normal price at a ripperdoc",
        width / 2,
        338,
        0xff79dae5);
  }
}
