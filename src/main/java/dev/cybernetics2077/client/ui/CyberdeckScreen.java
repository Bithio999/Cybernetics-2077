package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.deck.CyberdeckMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CyberdeckScreen extends AbstractContainerScreen<CyberdeckMenu> {
  public CyberdeckScreen(CyberdeckMenu menu, Inventory inv, Component title) {
    super(menu, inv, title);
    imageWidth = 276;
    imageHeight = 219;
    titleLabelY = -1000;
    inventoryLabelY = -1000;
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
      c.panel(0, 0, 276, 219, 8, 0xfb080e19, 0xffb44b59);
      c.glowText("CYBERDECK", 12, 9, 20, 0xffef6b77);
      c.right("TIER 1", 263, 15, 11, 0xffd2e8e9);
      c.line(12, 35, 264, 35, 1, 0xff693642);
      c.tracked("RAM", 15, 48, 9, 0xff70e1ee, .5f);
      for (int i = 0; i < 4; i++) {
        c.panel(15 + i * 18, 63, 14, 9, 2, 0xff235163, 0xff63cadd);
        c.line(18 + i * 18, 66, 24 + i * 18, 66, .5f, 0xff9aebf4);
      }
      c.body("4 units", 15, 81, 9, 0xffa0b9c4);
      c.text("QUICKHACK SLOTS", 106, 47, 12, 0xff7ce4ee);
      for (int i = 0; i < 2; i++) {
        c.panel(106 + i * 54, 61, 28, 28, 4, 0xff101b29, 0xff66868e);
        c.text("0" + (i + 1), 106 + i * 54, 94, 8, 0xff658695);
      }
      c.body("Shift-click a chip to install. One copy of each quickhack.", 15, 109, 9, 0xffa5b4c3);
      for (int row = 0; row < 3; row++)
        for (int col = 0; col < 9; col++)
          c.panel(57 + col * 18, 125 + row * 18, 18, 18, 2, 0xff09121c, 0xff374954);
      for (int col = 0; col < 9; col++)
        c.panel(57 + col * 18, 187, 18, 18, 2, 0xff101d28, 0xff526977);
      c.text("LOADOUT", 12, 134, 10, 0xffda7783);
      c.body("Chips stay", 12, 153, 8, 0xffa5b4c3);
      c.body("with this", 12, 164, 8, 0xffa5b4c3);
      c.body("cyberdeck.", 12, 175, 8, 0xffa5b4c3);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }
}
