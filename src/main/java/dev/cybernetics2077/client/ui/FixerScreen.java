package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.city.FixerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FixerScreen extends AbstractContainerScreen<FixerMenu> {
  public FixerScreen(FixerMenu m, Inventory i, Component title) {
    super(m, i, title);
    imageWidth = 318;
    imageHeight = 212;
    inventoryLabelY = titleLabelY = -1000;
  }

  @Override
  protected void init() {
    super.init();
    addRenderableWidget(
        new CyberActionButton(
            leftPos + 210, topPos + 181, 96, 19, Component.literal("LEAVE"), () -> onClose()));
  }

  @Override
  protected void renderBg(GuiGraphics g, float p, int x, int y) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      c.panel(0, 0, 318, 212, 8, 0xfc0a1717, 0xff5c9a78);
      c.glowText("AFTERLIFE // FIXER NETWORK", 12, 12, 15, 0xff9af2b6);
      c.line(12, 36, 306, 36, 1, 0xff476952);
      c.text(title.getString(), 12, 48, 13, 0xffd3dfcf);
      c.panel(12, 73, 146, 45, 4, 0xff12251f, 0xff345845);
      c.text("STREET CRED", 22, 83, 9, 0xff95b6a0);
      c.text(menu.streetCred() + " / 50", 22, 99, 12, 0xffbce7ad);
      c.panel(166, 73, 140, 45, 4, 0xff12251f, 0xff345845);
      c.text("EURODOLLARS", 176, 83, 9, 0xff95b6a0);
      c.text(
          "€$ " + String.format(java.util.Locale.ROOT, "%,d", menu.eddies()),
          180,
          99,
          12,
          0xffe7bf73);
      c.text("NO OPEN CONTRACTS", 12, 135, 13, 0xff9ed5bb);
      c.body("The network is quiet. Check back when work is available.", 12, 155, 9, 0xffa9b9af);
      c.text("MEETING POINT  /  AFTERLIFE", 12, 187, 8, 0xff718e7b);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float p) {
    super.render(g, x, y, p);
    renderTooltip(g, x, y);
  }
}
