package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CyberneticsBenchScreen extends AbstractContainerScreen<CyberneticsBenchMenu> {
  private CyberActionButton deckButton;

  public CyberneticsBenchScreen(CyberneticsBenchMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 314;
    imageHeight = 204;
    inventoryLabelY = titleLabelY = -1000;
  }

  @Override
  protected void init() {
    super.init();
    deckButton =
        addRenderableWidget(
            new CyberActionButton(
                leftPos + 10,
                topPos + 176,
                64,
                16,
                Component.literal("QUICKHACKS"),
                () -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0)));
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float partial) {
    deckButton.active = menu.hasDeck() && menu.getCarried().isEmpty();
    super.render(g, x, y, partial);
    renderTooltip(g, x, y);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      ServiceStyle.frame(c, imageWidth, imageHeight, ServiceStyle.CYAN);
      c.text("CYBERNETICS BENCH", 17, 7, 12, ServiceStyle.INK);
      c.right(
          menu.capacityUsed() + " / " + menu.capacityLimit() + " CAP",
          304,
          9,
          9,
          ServiceStyle.GOLD);
      for (var category : CyberwareCategory.values()) {
        float x = CyberneticsBenchMenu.x(category), y = CyberneticsBenchMenu.y(category);
        c.body(
            c.fit(I18n.get("cybernetics2077.bench." + category.id), 70, 9, false),
            x,
            y + 4,
            9,
            ServiceStyle.MUTED);
        for (int n = 0; n < category.sockets; n++) {
          float sx = x + 75 + n * 18;
          ServiceStyle.slot(c, sx, y - 1, false);
          var item = menu.getSlot(category.firstSlot() + n).getItem();
          if (item.isEmpty()) CyberGlyphs.category(c, category, sx + 4, y + 3, 9, 0x50669aa8);
          else if (item.getItem() instanceof CyberwareItem i)
            c.rect(sx + 1, y + 15, 16, 1, i.tier().color);
        }
      }
      c.body("INVENTORY", 84, 113, 6, ServiceStyle.MUTED);
      for (int row = 0; row < 3; row++)
        for (int col = 0; col < 9; col++)
          ServiceStyle.slot(c, 83 + col * 18, 118 + row * 18, false);
      for (int col = 0; col < 9; col++) ServiceStyle.slot(c, 83 + col * 18, 176, true);
      c.text("INSTALL", 10, 129, 9, ServiceStyle.CYAN);
      c.body("Shift-click", 10, 143, 8, ServiceStyle.MUTED);
      c.body("to equip/remove.", 10, 154, 8, ServiceStyle.MUTED);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }
}
