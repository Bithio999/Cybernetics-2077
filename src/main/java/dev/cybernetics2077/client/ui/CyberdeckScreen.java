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
    imageHeight = 227;
    titleLabelY = -1000;
    inventoryLabelY = -1000;
  }

  @Override
  protected void init() {
    super.init();
    addRenderableWidget(
        new CyberActionButton(
            leftPos + 12, topPos + 9, 26, 20, Component.literal("<"), this::onClose));
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
  protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      c.panel(0, 0, 276, 227, 8, 0xfb080e19, 0xffb44b59);
      c.glowText("QUICKHACKS", 46, 9, 20, 0xffef6b77);
      c.right("TIER " + menu.tier().level, 263, 15, 11, menu.tier().color);
      c.line(12, 35, 264, 35, 1, 0xff693642);
      c.tracked("RAM", 15, 48, 9, 0xff70e1ee, .5f);
      for (int i = 0; i < menu.tier().ram(); i++) {
        int x = 15 + i % 4 * 18, y = 61 + i / 4 * 13;
        c.panel(x, y, 14, 9, 2, 0xff235163, 0xff63cadd);
        c.line(x + 3, y + 3, x + 9, y + 3, .5f, 0xff9aebf4);
      }
      c.body(menu.tier().ram() + " units", 15, 103, 9, 0xffa0b9c4);
      c.text("QUICKHACK SLOTS", 106, 42, 12, 0xff7ce4ee);
      for (int i = 0; i < menu.programSlots(); i++) {
        int x = CyberdeckMenu.slotX(i), y = CyberdeckMenu.slotY(i);
        c.panel(x - 6, y - 6, 28, 28, 4, 0xff101b29, 0xff66868e);
        c.text("" + (i + 1), x + 15, y + 15, 6, 0xff658695);
      }
      c.body(
          "Chips: tier " + menu.tier().level + " or lower. One per program.",
          106,
          119,
          8,
          0xffa5b4c3);
      for (int row = 0; row < 3; row++)
        for (int col = 0; col < 9; col++)
          c.panel(57 + col * 18, 133 + row * 18, 18, 18, 2, 0xff09121c, 0xff374954);
      for (int col = 0; col < 9; col++)
        c.panel(57 + col * 18, 195, 18, 18, 2, 0xff101d28, 0xff526977);
      c.text("LOADOUT", 12, 142, 10, 0xffda7783);
      c.body("Chips stay", 12, 161, 8, 0xffa5b4c3);
      c.body("with this", 12, 172, 8, 0xffa5b4c3);
      c.body("cyberdeck.", 12, 183, 8, 0xffa5b4c3);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }
}
