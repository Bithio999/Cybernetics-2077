package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.deck.CyberdeckMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CyberdeckScreen extends AbstractContainerScreen<CyberdeckMenu> {
  public CyberdeckScreen(CyberdeckMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 220;
    imageHeight = 202;
    titleLabelY = inventoryLabelY = -1000;
  }

  @Override
  protected void init() {
    super.init();
    addRenderableWidget(
        new CyberActionButton(
            leftPos + 192, topPos + 6, 18, 14, Component.literal("<"), this::onClose));
  }

  @Override
  public void onClose() {
    if (!menu.getCarried().isEmpty()) {
      super.onClose();
      return;
    }
    CyberwareScreen.returnToOperatingSystem = true;
    net.neoforged.neoforge.network.PacketDistributor.sendToServer(
        dev.cybernetics2077.network.OpenCyberware.INSTANCE);
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float partial) {
    super.render(g, x, y, partial);
    renderTooltip(g, x, y);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics g, float partial, int x, int y) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      ServiceStyle.frame(c, imageWidth, imageHeight, 0xffdf6c78);
      c.text("QUICKHACK LOADOUT", 17, 7, 12, ServiceStyle.INK);
      c.body(
          c.fit(menu.cyberdeck().getDescription().getString(), 166, 9, false),
          12,
          29,
          9,
          ServiceStyle.CYAN);
      c.right("T" + menu.tier().level, 208, 29, 9, menu.tier().color);
      for (int i = 0; i < menu.programSlots(); i++) {
        int sx = CyberdeckMenu.slotX(i), sy = CyberdeckMenu.slotY(i);
        ServiceStyle.slot(c, sx - 1, sy - 1, false);
        c.body("" + (i + 1), sx + 18, sy + 5, 6, ServiceStyle.MUTED);
      }
      c.text("RAM", 138, 48, 8, ServiceStyle.MUTED);
      c.text("" + menu.cyberdeck().ram(), 138, 60, 20, ServiceStyle.CYAN);
      c.body("Queue " + menu.tier().queueLimit(), 138, 85, 8, ServiceStyle.MUTED);
      c.body("Tier " + menu.tier().level + " chips or lower", 12, 100, 8, ServiceStyle.MUTED);
      c.line(12, 111, 208, 111, .5f, ServiceStyle.LINE);
      for (int row = 0; row < 3; row++)
        for (int col = 0; col < 9; col++)
          ServiceStyle.slot(c, 29 + col * 18, 115 + row * 18, false);
      for (int col = 0; col < 9; col++) ServiceStyle.slot(c, 29 + col * 18, 173, true);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }
}
