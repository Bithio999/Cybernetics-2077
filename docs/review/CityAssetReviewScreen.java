package dev.cybernetics2077.review;

import dev.cybernetics2077.city.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Test-only native entity and inventory-model contact sheets, never packaged in the mod. */
final class CityAssetReviewScreen extends Screen {
  private final boolean people;
  private final CityResident[] residents = new CityResident[4];

  CityAssetReviewScreen(boolean people) {
    super(Component.literal(people ? "Residents" : "Furniture"));
    this.people = people;
    if (!people) checkModels();
    if (people)
      for (int n = 0; n < 4; n++) {
        residents[n] =
            (n < 2 ? CityContent.RIPPERDOC : CityContent.FIXER)
                .get()
                .create(Minecraft.getInstance().level);
        var tag = new CompoundTag();
        tag.putInt("CityStyle", n % 2);
        tag.putFloat("Health", 40);
        residents[n].readAdditionalSaveData(tag);
        residents[n].setNoAi(true);
      }
  }

  private static void checkModels() {
    var mc = Minecraft.getInstance();
    int checked = 0;
    for (var entry : CityContent.BLOCK_ITEMS.entrySet()) {
      var stack = new ItemStack(entry.getValue().get());
      var baked = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
      if (baked == mc.getModelManager().getMissingModel())
        throw new IllegalStateException("Missing furniture item model " + entry.getKey());
      for (var context : ItemDisplayContext.values()) {
        if (context == ItemDisplayContext.NONE || context == ItemDisplayContext.HEAD) continue;
        var pose = new com.mojang.blaze3d.vertex.PoseStack();
        var transform = baked.getTransforms().getTransform(context);
        transform.apply(
            context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            pose);
        pose.translate(-.5, -.5, -.5);
        var lo = new org.joml.Vector3f(Float.POSITIVE_INFINITY);
        var hi = new org.joml.Vector3f(Float.NEGATIVE_INFINITY);
        int count = 0;
        for (int face = -1; face < 6; face++)
          for (var q :
              baked.getQuads(
                  null,
                  face < 0 ? null : net.minecraft.core.Direction.values()[face],
                  net.minecraft.util.RandomSource.create(0))) {
            var data = q.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
              int k = vertex * stride;
              var point =
                  new org.joml.Vector3f(
                      Float.intBitsToFloat(data[k]),
                      Float.intBitsToFloat(data[k + 1]),
                      Float.intBitsToFloat(data[k + 2]));
              pose.last().pose().transformPosition(point);
              lo.min(point);
              hi.max(point);
              count++;
            }
          }
        float span =
            hi.sub(lo, new org.joml.Vector3f())
                .get(hi.sub(lo, new org.joml.Vector3f()).maxComponent());
        float budget =
            context == ItemDisplayContext.GUI
                ? 1.15f
                : context == ItemDisplayContext.GROUND
                    ? .6f
                    : context == ItemDisplayContext.FIXED ? .95f : .8f;
        if (count == 0 || !Float.isFinite(span) || span > budget)
          throw new IllegalStateException(
              "Furniture display out of bounds: " + entry.getKey() + " " + context + " " + span);
      }
      checked++;
    }
    System.out.println(
        "PASS: native baked inventory, both first/third-person hands, ground and frame bounds for "
            + checked
            + " furniture items");
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    g.fill(0, 0, width, height, 0xff0c151d);
    g.drawString(
        font,
        people
            ? "VILLAGE RESIDENTS / NATIVE MINECRAFT MODELS"
            : "FURNITURE / NATIVE INVENTORY MODELS",
        16,
        12,
        0xffa8e2ce,
        false);
    if (people)
      for (int n = 0; n < 4; n++) {
        int cw = (width - 32) / 4, x = 16 + n * cw;
        g.fill(x, 34, x + cw - 8, height - 29, 0xff15252d);
        InventoryScreen.renderEntityInInventoryFollowsAngle(
            g,
            x,
            38,
            x + cw - 8,
            height - 46,
            Math.min(100, (height - 110) / 2),
            0,
            -.38f,
            .025f,
            residents[n]);
        var text = residents[n].getName().getString().split(" · ")[0];
        g.drawCenteredString(font, text, x + (cw - 8) / 2, height - 42, 0xffe1dfc9);
        g.drawCenteredString(
            font, n < 2 ? "RIPPERDOC" : "FIXER", x + (cw - 8) / 2, height - 25, 0xff78b7b4);
      }
    else {
      int n = 0;
      for (var entry : CityContent.BLOCK_ITEMS.entrySet()) {
        int col = n % 6,
            row = n / 6,
            cw = (width - 32) / 6,
            rh = (height - 53) / ((CityContent.BLOCK_ITEMS.size() + 5) / 6),
            x = 16 + col * cw,
            y = 38 + row * rh;
        g.fill(x, y, x + cw - 5, y + rh - 5, 0xff15252d);
        g.pose().pushPose();
        g.pose().translate(x + cw / 2f - 16, y + 3, 0);
        g.pose().scale(2, 2, 2);
        var stack = new ItemStack(entry.getValue().get());
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        g.pose().pushPose();
        g.pose().translate(x + 3, y + rh - 18, 0);
        g.pose().scale(.65f, .65f, .65f);
        g.drawString(font, entry.getKey().replace('_', ' '), 0, 0, 0xffb4cbcb, false);
        g.pose().popPose();
        n++;
      }
    }
    g.drawString(
        font,
        "Review fixture — inventory entity/item renderer, no concept rendering",
        16,
        height - 12,
        0xff758c91,
        false);
  }
}
