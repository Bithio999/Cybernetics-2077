package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CyberneticsBenchScreen extends AbstractContainerScreen<CyberneticsBenchMenu> {
  public CyberneticsBenchScreen(CyberneticsBenchMenu menu, Inventory inv, Component title) {
    super(menu, inv, title);
    imageWidth = 330;
    imageHeight = 234;
    inventoryLabelY = -1000;
    titleLabelY = -1000;
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float partial) {
    super.render(g, x, y, partial);
    renderTooltip(g, x, y);
  }

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      c.panel(0, 0, imageWidth, imageHeight, 7, 0xfb0b101b, 0xffb74b59);
      c.glowText("CYBERNETICS BENCH", 10, 6, 16, 0xfff17a83);
      c.right(
          menu.capacityUsed() + " / " + Cybernetics2077.CAPACITY.get() + " CAPACITY",
          319,
          10,
          10,
          0xffe6bb69);
      c.line(10, 25, 320, 25, .6f, 0xff74343f);
      for (var category : CyberwareCategory.values()) {
        float x = CyberneticsBenchMenu.x(category), y = CyberneticsBenchMenu.y(category);
        String label = I18n.get("cybernetics2077.bench." + category.id);
        c.text(label, x, y + 4, 11, 0xffc9bbc9);
        for (int n = 0; n < category.sockets; n++) {
          float xx = x + 75 + n * 18;
          c.shape(Geometry.panel(xx, y - 1, 18, 18, 2), 0xff071019, 0xff614350);
          var item = menu.getSlot(category.firstSlot() + n).getItem();
          if (item.isEmpty()) CyberGlyphs.category(c, category, xx + 4, y + 3, 10, 0x6866a5b5);
          else if (item.getItem() instanceof CyberwareItem i)
            c.rect(xx, y + 15, 17, 1, i.tier().color);
        }
      }
      c.tracked("INVENTORY  /  SHIFT-CLICK TO INSTALL OR REMOVE", 84, 133, 7, 0xff6cbbc9, .2f);
      for (int row = 0; row < 3; row++)
        for (int col = 0; col < 9; col++)
          c.panel(83 + col * 18, 142 + row * 18, 18, 18, 2, 0xff070e16, 0xff394752);
      for (int col = 0; col < 9; col++)
        c.panel(83 + col * 18, 204, 18, 18, 2, 0xff101b26, 0xff577080);
      c.text("INSTALL", 10, 153, 11, 0xff74dfe9);
      c.body("Match the", 10, 171, 9, 0xff9c9caa);
      c.body("body system.", 10, 183, 9, 0xff9c9caa);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }
}
