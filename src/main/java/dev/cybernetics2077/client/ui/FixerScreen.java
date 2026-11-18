package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.city.FixerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FixerScreen extends AbstractContainerScreen<FixerMenu> {
  private CyberActionButton accept;

  public FixerScreen(FixerMenu m, Inventory i, Component title) {
    super(m, i, title);
    imageWidth = 318;
    imageHeight = 234;
    inventoryLabelY = titleLabelY = -1000;
  }

  @Override
  protected void init() {
    super.init();
    accept =
        addRenderableWidget(
            new CyberActionButton(
                leftPos + 12,
                topPos + 202,
                190,
                19,
                Component.literal("ACCEPT CONTRACT"),
                () -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1)));
    addRenderableWidget(
        new CyberActionButton(
            leftPos + 210, topPos + 202, 96, 19, Component.literal("LEAVE"), () -> onClose()));
  }

  @Override
  protected void renderBg(GuiGraphics g, float p, int x, int y) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      c.panel(0, 0, 318, 234, 8, 0xfc0a1717, 0xff5c9a78);
      c.glowText("AFTERLIFE // FIXER NETWORK", 12, 12, 15, 0xff9af2b6);
      c.line(12, 36, 306, 36, 1, 0xff476952);
      c.text(title.getString(), 12, 46, 13, 0xffd3dfcf);
      c.panel(12, 65, 146, 37, 4, 0xff12251f, 0xff345845);
      c.text("STREET CRED", 22, 71, 8, 0xff95b6a0);
      c.text(menu.streetCred() + " / 50", 22, 85, 12, 0xffbce7ad);
      c.panel(166, 65, 140, 37, 4, 0xff12251f, 0xff345845);
      c.text("EURODOLLARS", 176, 71, 8, 0xff95b6a0);
      c.text(
          "€$ " + String.format(java.util.Locale.ROOT, "%,d", menu.eddies()),
          176,
          85,
          12,
          0xffe7bf73);
      c.panel(12, 110, 294, 78, 4, 0xff101e1b, 0xff345845);
      c.text("FIRST IMPRESSIONS", 22, 118, 13, 0xff9ed5bb);
      c.right(
          menu.stage() == 2 ? "COMPLETED" : menu.stage() == 1 ? "ACTIVE" : "DELIVERY",
          296,
          121,
          8,
          0xffe7bf73);
      c.body("Deliver a sealed diagnostic packet to a ripperdoc.", 22, 140, 9, 0xffbdcfc3);
      c.body("Use the contract shard on the doctor to hand it over.", 22, 154, 9, 0xffbdcfc3);
      c.text("€$ 1,000    /    +5 STREET CRED    /    ONE-TIME", 22, 174, 9, 0xffe7bf73);
      String status =
          menu.result() == 2
              ? "Free an inventory slot, then try again."
              : menu.result() == 1
                  ? "No clinic route available. Visit a clinic and return."
                  : menu.stage() == 2
                      ? "Delivery complete. Thanks for keeping it clean."
                      : menu.stage() == 1
                          ? "Hold the scan key for your route. Lost shard? Reissue it."
                          : !menu.hasRoute()
                              ? "Find a ripperdoc clinic nearby, then return for work."
                              : "Virtual cargo. No additional inventory space needed.";
      c.body(status, 12, 191, 8, 0xff9eb7a5);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }

  @Override
  public void render(GuiGraphics g, int x, int y, float p) {
    accept.active = menu.stage() != 2 && (menu.stage() == 1 || menu.hasRoute());
    accept.setMessage(
        Component.literal(
            menu.stage() == 2
                ? "DELIVERY COMPLETE"
                : menu.stage() == 1 ? "REISSUE CONTRACT" : "ACCEPT CONTRACT"));
    super.render(g, x, y, p);
    renderTooltip(g, x, y);
  }
}
